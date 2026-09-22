package com.aslenix.attendance.service;

import com.aslenix.attendance.dto.EvaluationResult;
import com.aslenix.attendance.entity.*;
import com.aslenix.attendance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Employee of the Month scoring engine.
 *
 * The algorithm is intentionally modular: attendance scoring, task scoring and
 * ranking are separate steps, and every numeric rule lives in
 * {@link PerformanceSettings} so it can be changed from the admin panel without
 * touching this code.
 *
 * Final score = attendanceScore * attendanceWeight% + taskScore * taskWeight%,
 * always clamped to [0, 100].
 */
@Service
public class PerformanceScoringService {

    private static final List<String> COMPLETED_STATUSES = List.of("COMPLETED", "VERIFIED");

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskAssignmentHistoryRepository taskAssignmentHistoryRepository;
    private final OfficeSettingsRepository officeSettingsRepository;
    private final PerformanceSettingsRepository performanceSettingsRepository;
    private final EmployeePerformanceScoreRepository scoreRepository;
    private final EmployeeOfTheMonthRepository winnerRepository;
    private final NotificationService notificationService;

    public PerformanceScoringService(EmployeeRepository employeeRepository,
                                     AttendanceRepository attendanceRepository,
                                     TaskAssignmentRepository taskAssignmentRepository,
                                     TaskAssignmentHistoryRepository taskAssignmentHistoryRepository,
                                     OfficeSettingsRepository officeSettingsRepository,
                                     PerformanceSettingsRepository performanceSettingsRepository,
                                     EmployeePerformanceScoreRepository scoreRepository,
                                     EmployeeOfTheMonthRepository winnerRepository,
                                     NotificationService notificationService) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskAssignmentHistoryRepository = taskAssignmentHistoryRepository;
        this.officeSettingsRepository = officeSettingsRepository;
        this.performanceSettingsRepository = performanceSettingsRepository;
        this.scoreRepository = scoreRepository;
        this.winnerRepository = winnerRepository;
        this.notificationService = notificationService;
    }

    // ============================================================
    // PUBLIC API
    // ============================================================

    /**
     * Evaluates every eligible employee for the given month, persists the
     * results permanently and selects the Employee of the Month.
     * Re-running replaces that month's stored results.
     */
    @Transactional
    public EvaluationResult evaluateMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        PerformanceSettings settings = performanceSettingsRepository
                .findFirstByOrderByIdAsc()
                .orElseGet(() -> performanceSettingsRepository.save(new PerformanceSettings()));

        OfficeSettings office = officeSettingsRepository.findFirstByOrderByIdAsc().orElse(null);
        LocalTime workStart = office != null && office.getWorkStartTime() != null
                ? office.getWorkStartTime() : LocalTime.of(10, 0);
        LocalTime workEnd = office != null && office.getWorkEndTime() != null
                ? office.getWorkEndTime() : LocalTime.of(18, 0);
        long scheduledMinutes = Math.max(0, Duration.between(workStart, workEnd).toMinutes());

        List<Employee> employees = employeeRepository.findByUserRole(Role.EMPLOYEE).stream()
                .filter(Employee::isEnabled)
                .toList();

        List<EmployeePerformanceScore> scores = new ArrayList<>();
        for (Employee employee : employees) {
            scores.add(evaluateEmployee(employee, firstDay, lastDay, settings, workStart, scheduledMinutes));
        }

        // --------------------------------------------------------
        // RANK + SELECT WINNER
        // --------------------------------------------------------
        scores.sort(rankingComparator());
        for (int i = 0; i < scores.size(); i++) {
            scores.get(i).setRank(i + 1);
            scores.get(i).setEmployeeOfTheMonth(false);
            scores.get(i).setTieBreakerUsed(null);
        }

        EmployeeOfTheMonth winner = null;
        if (!scores.isEmpty()) {
            EmployeePerformanceScore top = scores.get(0);
            EmployeePerformanceScore runnerUp = scores.size() > 1 ? scores.get(1) : null;
            String tieBreaker = resolveTieBreakerLabel(top, runnerUp);
            top.setEmployeeOfTheMonth(true);
            top.setTieBreakerUsed(tieBreaker);

            winner = new EmployeeOfTheMonth();
            winner.setEmployee(top.getEmployee());
            winner.setEmployeeName(top.getEmployeeName());
            winner.setDepartmentName(top.getDepartmentName());
            winner.setEvalYear(year);
            winner.setEvalMonth(month);
            winner.setFinalScore(top.getFinalScore());
            winner.setAttendanceScore(top.getAttendanceScore());
            winner.setTaskScore(top.getTaskScore());
            winner.setTieBreakerUsed(tieBreaker);
            winner.setCalculatedAt(LocalDateTime.now());
        }

        // --------------------------------------------------------
        // PERSIST (replace this month's stored results)
        // --------------------------------------------------------
        List<EmployeePerformanceScore> existing = scoreRepository.findByEvalYearAndEvalMonth(year, month);
        if (!existing.isEmpty()) {
            scoreRepository.deleteAll(existing);
            scoreRepository.flush();
        }
        // Flush the winner delete before saving the new one: Hibernate orders
        // inserts before deletes within a flush, which would otherwise violate
        // the unique (eval_year, eval_month) constraint on re-evaluation.
        winnerRepository.findByEvalYearAndEvalMonth(year, month).ifPresent(w -> {
            winnerRepository.delete(w);
            winnerRepository.flush();
        });

        List<EmployeePerformanceScore> saved = scoreRepository.saveAll(scores);

        if (winner != null) {
            winnerRepository.save(winner);
            notifyWinner(winner, ym);
        }

        return new EvaluationResult(
                year,
                month,
                saved.size(),
                winner != null ? winner.getEmployeeName() : null,
                winner != null ? winner.getFinalScore() : 0,
                winner != null
        );
    }

    @Transactional(readOnly = true)
    public List<EmployeePerformanceScore> getMonthResults(int year, int month) {
        return scoreRepository.findByEvalYearAndEvalMonthOrderByRankAsc(year, month);
    }

    @Transactional(readOnly = true)
    public EmployeePerformanceScore getEmployeeScore(Long employeeId, int year, int month) {
        return scoreRepository.findByEmployeeIdAndEvalYearAndEvalMonth(employeeId, year, month)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PerformanceTaskBreakdown> getTaskBreakdown(Long scoreId) {
        return scoreRepository.findById(scoreId)
                .map(EmployeePerformanceScore::getTaskBreakdowns)
                .orElse(List.of());
    }

    // ============================================================
    // PER-EMPLOYEE EVALUATION
    // ============================================================

    private EmployeePerformanceScore evaluateEmployee(Employee employee,
                                                      LocalDate firstDay,
                                                      LocalDate lastDay,
                                                      PerformanceSettings settings,
                                                      LocalTime workStart,
                                                      long scheduledMinutes) {

        EmployeePerformanceScore score = new EmployeePerformanceScore();
        score.setEmployee(employee);
        score.setEmployeeName(fullName(employee));
        score.setDepartmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : "—");
        score.setEvalYear(firstDay.getYear());
        score.setEvalMonth(firstDay.getMonthValue());
        score.setAttendanceWeightUsed(settings.getAttendanceWeight());
        score.setTaskWeightUsed(settings.getTaskWeight());
        score.setCalculatedAt(LocalDateTime.now());

        AttendanceOutcome attendance = scoreAttendance(employee, firstDay, lastDay, settings, workStart, scheduledMinutes);
        score.setLateCheckIns(attendance.lateCheckIns);
        score.setEarlyCheckIns(attendance.earlyCheckIns);
        score.setOvertimeHours(round(attendance.overtimeHours));
        score.setDaysPresent(attendance.daysPresent);
        score.setRawAttendancePoints(round(attendance.rawPoints));
        score.setAttendanceScore(attendance.score);

        TaskOutcome task = scoreTasks(employee, firstDay, lastDay, settings, score);
        score.setTasksCompleted(task.tasksCompleted);
        score.setSmallCompleted(task.smallCompleted);
        score.setMediumCompleted(task.mediumCompleted);
        score.setLargeCompleted(task.largeCompleted);
        score.setEpicCompleted(task.epicCompleted);
        score.setTasksEarly(task.tasksEarly);
        score.setTasksOnTime(task.tasksOnTime);
        score.setTasksLate(task.tasksLate);
        score.setTasksNoDeadline(task.tasksNoDeadline);
        score.setRawTaskPoints(round(task.rawPoints));
        score.setTaskScore(task.score);

        double finalScore = clamp(
                attendance.score * settings.getAttendanceWeight() / 100.0
                        + task.score * settings.getTaskWeight() / 100.0);
        score.setFinalScore(round(finalScore));

        return score;
    }

    // ============================================================
    // ATTENDANCE SCORING (30%)
    // ============================================================

    private AttendanceOutcome scoreAttendance(Employee employee,
                                              LocalDate firstDay,
                                              LocalDate lastDay,
                                              PerformanceSettings settings,
                                              LocalTime workStart,
                                              long scheduledMinutes) {

        List<Attendance> records =
                attendanceRepository.findByEmployeeAndAttendanceDateBetween(employee, firstDay, lastDay);

        int late = 0;
        int early = 0;
        int present = 0;
        long overtimeMinutes = 0;

        for (Attendance a : records) {
            if (a.getCheckIn() == null) {
                continue;
            }
            present++;

            if (a.isLate() || "LATE".equalsIgnoreCase(a.getStatus())) {
                late++;
            }
            if (a.getCheckIn().toLocalTime().isBefore(workStart)) {
                early++;
            }
            if (a.getCheckOut() != null) {
                long actual = Duration.between(a.getCheckIn(), a.getCheckOut()).toMinutes();
                overtimeMinutes += Math.max(0, actual - scheduledMinutes);
            }
        }

        double overtimeHours = overtimeMinutes / 60.0;

        // Edge case: no attendance at all -> no positive attendance contribution.
        if (present == 0) {
            return new AttendanceOutcome(0, 0, 0, 0, 0, 0);
        }

        double cappedOtHours = settings.getMaxOvertimeBonusHours() > 0
                ? Math.min(overtimeHours, settings.getMaxOvertimeBonusHours())
                : overtimeHours;

        double raw = 100
                - late * settings.getLateDeductionPoints()
                + early * settings.getEarlyCheckInBonusPoints()
                + cappedOtHours * settings.getOvertimeBonusPerHour();

        return new AttendanceOutcome(late, early, present, overtimeHours, raw, clamp(raw));
    }

    // ============================================================
    // TASK SCORING (70%)
    // ============================================================

    private TaskOutcome scoreTasks(Employee employee,
                                   LocalDate firstDay,
                                   LocalDate lastDay,
                                   PerformanceSettings settings,
                                   EmployeePerformanceScore score) {

        List<TaskAssignment> completed =
                taskAssignmentRepository.findByAssigneeAndStatusIn(employee, COMPLETED_STATUSES);

        TaskOutcome outcome = new TaskOutcome();
        double rawPoints = 0;

        for (TaskAssignment assignment : completed) {
            LocalDateTime completedAt = resolveCompletionDate(assignment);
            if (completedAt == null) {
                continue;
            }
            LocalDate completedDate = completedAt.toLocalDate();
            if (completedDate.isBefore(firstDay) || completedDate.isAfter(lastDay)) {
                continue;
            }

            String complexity = assignment.getWeight() != null ? assignment.getWeight() : "MEDIUM";
            double basePoints = settings.complexityPoints(complexity);

            switch (complexity.trim().toUpperCase()) {
                case "SMALL" -> outcome.smallCompleted++;
                case "LARGE" -> outcome.largeCompleted++;
                case "EPIC" -> outcome.epicCompleted++;
                default -> outcome.mediumCompleted++;
            }

            LocalDate deadline = assignment.getDeadline();
            String result;
            double adjustment;
            long daysDifference;

            if (deadline == null) {
                result = "NO_DEADLINE";
                adjustment = 0;
                daysDifference = 0;
                outcome.tasksNoDeadline++;
            } else {
                // Positive => completed early, negative => completed late.
                daysDifference = ChronoUnit.DAYS.between(completedDate, deadline);
                if (daysDifference > 0) {
                    result = "EARLY";
                    adjustment = settings.getEarlyCompletionBonusPoints();
                    outcome.tasksEarly++;
                } else if (daysDifference < 0) {
                    result = "LATE";
                    adjustment = -settings.getLateCompletionDeductionPoints();
                    outcome.tasksLate++;
                } else {
                    result = "ON_TIME";
                    adjustment = settings.getOnTimeCompletionPoints();
                    outcome.tasksOnTime++;
                }
            }

            double total = basePoints + adjustment;
            rawPoints += total;
            outcome.tasksCompleted++;

            PerformanceTaskBreakdown line = new PerformanceTaskBreakdown();
            line.setTaskAssignmentId(assignment.getId());
            line.setTaskTitle(assignment.getTask() != null ? assignment.getTask().getTitle() : "—");
            line.setAssignmentTitle(assignment.getTitle());
            line.setComplexity(complexity.trim().toUpperCase());
            line.setBasePoints(basePoints);
            line.setDeadlineOutcome(result);
            line.setDeadline(deadline);
            line.setCompletedAt(completedAt);
            line.setDaysDifference(daysDifference);
            line.setDeadlineAdjustment(adjustment);
            line.setTotalPoints(total);
            score.addTaskBreakdown(line);
        }

        outcome.rawPoints = rawPoints;
        if (outcome.tasksCompleted == 0) {
            outcome.score = 0;
        } else {
            double perfect = settings.getTaskPerfectScorePoints() > 0
                    ? settings.getTaskPerfectScorePoints() : 100.0;
            outcome.score = clamp(rawPoints / perfect * 100.0);
        }
        return outcome;
    }

    /**
     * Completion timestamp for a work item: the earliest history entry that
     * reached 100% progress, falling back to the assignment's last update.
     */
    private LocalDateTime resolveCompletionDate(TaskAssignment assignment) {
        List<TaskAssignmentHistory> histories =
                taskAssignmentHistoryRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId());

        LocalDateTime earliest = null;
        for (TaskAssignmentHistory h : histories) {
            if (h.getNewProgress() != null && h.getNewProgress() >= 100 && h.getCreatedAt() != null) {
                if (earliest == null || h.getCreatedAt().isBefore(earliest)) {
                    earliest = h.getCreatedAt();
                }
            }
        }
        if (earliest != null) {
            return earliest;
        }
        return assignment.getUpdatedAt();
    }

    // ============================================================
    // RANKING + TIE BREAKERS
    // ============================================================

    private Comparator<EmployeePerformanceScore> rankingComparator() {
        return Comparator
                .comparingDouble(EmployeePerformanceScore::getFinalScore).reversed()
                .thenComparing(Comparator.comparingDouble(EmployeePerformanceScore::getTaskScore).reversed())
                .thenComparing(Comparator.comparingDouble(EmployeePerformanceScore::getAttendanceScore).reversed())
                .thenComparing(Comparator.comparingInt(this::highComplexityCount).reversed())
                .thenComparing(Comparator.comparingInt(EmployeePerformanceScore::getTasksEarly).reversed())
                .thenComparing(s -> s.getEmployee() != null && s.getEmployee().getEmployeeCode() != null
                        ? s.getEmployee().getEmployeeCode() : "")
                .thenComparing(s -> s.getEmployee() != null && s.getEmployee().getId() != null
                        ? s.getEmployee().getId() : 0L);
    }

    private int highComplexityCount(EmployeePerformanceScore s) {
        return s.getLargeCompleted() + s.getEpicCompleted();
    }

    /**
     * Records which criterion separated the winner from the runner-up.
     * Returns "NONE" when the winner is the unique top scorer.
     */
    private String resolveTieBreakerLabel(EmployeePerformanceScore top, EmployeePerformanceScore runnerUp) {
        if (runnerUp == null || notEqual(top.getFinalScore(), runnerUp.getFinalScore())) {
            return "NONE";
        }
        if (notEqual(top.getTaskScore(), runnerUp.getTaskScore())) {
            return "HIGHER_TASK_SCORE";
        }
        if (notEqual(top.getAttendanceScore(), runnerUp.getAttendanceScore())) {
            return "HIGHER_ATTENDANCE_SCORE";
        }
        if (highComplexityCount(top) != highComplexityCount(runnerUp)) {
            return "MORE_HIGH_COMPLEXITY_TASKS";
        }
        if (top.getTasksEarly() != runnerUp.getTasksEarly()) {
            return "MORE_EARLY_COMPLETIONS";
        }
        return "EMPLOYEE_CODE";
    }

    private boolean notEqual(double a, double b) {
        return Math.abs(a - b) > 0.0001;
    }

    // ============================================================
    // NOTIFICATION
    // ============================================================

    private void notifyWinner(EmployeeOfTheMonth winner, YearMonth ym) {
        try {
            String monthLabel = ym.getMonth().name() + " " + ym.getYear();
            if (winner.getEmployee() != null) {
                notificationService.createNotification(
                        winner.getEmployee(),
                        "Employee of the Month — " + monthLabel,
                        "Congratulations! You have been selected as Employee of the Month for "
                                + monthLabel + " with a score of " + round(winner.getFinalScore()) + "/100.",
                        "EMPLOYEE_OF_THE_MONTH");
            }
            notificationService.createAdminNotification(
                    "Employee of the Month — " + monthLabel,
                    winner.getEmployeeName() + " selected as Employee of the Month ("
                            + round(winner.getFinalScore()) + "/100).",
                    "EMPLOYEE_OF_THE_MONTH");
        } catch (Exception ignored) {
            // Notifications are best-effort and must never fail the evaluation.
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String fullName(Employee employee) {
        String first = employee.getFirstName() != null ? employee.getFirstName() : "";
        String last = employee.getLastName() != null ? employee.getLastName() : "";
        return (first + " " + last).trim();
    }

    // ============================================================
    // RESULT HOLDERS
    // ============================================================

    private record AttendanceOutcome(int lateCheckIns,
                                     int earlyCheckIns,
                                     int daysPresent,
                                     double overtimeHours,
                                     double rawPoints,
                                     double score) {
    }

    private static final class TaskOutcome {
        int tasksCompleted;
        int smallCompleted;
        int mediumCompleted;
        int largeCompleted;
        int epicCompleted;
        int tasksEarly;
        int tasksOnTime;
        int tasksLate;
        int tasksNoDeadline;
        double rawPoints;
        double score;
    }
}
