package com.aslenix.attendance.controller;

import com.aslenix.attendance.dto.EvaluationResult;
import com.aslenix.attendance.dto.MonthOption;
import com.aslenix.attendance.entity.*;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeOfTheMonthRepository;
import com.aslenix.attendance.repository.EmployeePerformanceScoreRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.PerformanceScoringService;
import com.aslenix.attendance.service.PerformanceSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin-facing Employee of the Month dashboard, employee performance detail,
 * evaluation settings and manual evaluation trigger.
 */
@Controller
@RequestMapping("/admin/performance")
public class AdminPerformanceController {

    private static final int MONTH_HISTORY = 24;

    private final PerformanceScoringService scoringService;
    private final PerformanceSettingsService settingsService;
    private final EmployeePerformanceScoreRepository scoreRepository;
    private final EmployeeOfTheMonthRepository winnerRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public AdminPerformanceController(PerformanceScoringService scoringService,
                                      PerformanceSettingsService settingsService,
                                      EmployeePerformanceScoreRepository scoreRepository,
                                      EmployeeOfTheMonthRepository winnerRepository,
                                      EmployeeRepository employeeRepository,
                                      DepartmentRepository departmentRepository) {
        this.scoringService = scoringService;
        this.settingsService = settingsService;
        this.scoreRepository = scoreRepository;
        this.winnerRepository = winnerRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    // ============================================================
    // DASHBOARD
    // ============================================================

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@RequestParam(name = "year", required = false) Integer year,
                            @RequestParam(name = "month", required = false) Integer month,
                            @RequestParam(name = "employeeId", required = false) Long employeeId,
                            @RequestParam(name = "departmentId", required = false) Long departmentId,
                            Model model) {

        YearMonth current = YearMonth.now();
        int selYear = year != null ? year : current.getYear();
        int selMonth = month != null ? month : current.getMonthValue();
        if (selMonth < 1 || selMonth > 12) {
            selMonth = current.getMonthValue();
        }

        List<EmployeePerformanceScore> results = scoringService.getMonthResults(selYear, selMonth);

        // --------------------------------------------------------
        // FILTERS (employee / department)
        // --------------------------------------------------------
        String departmentFilter = null;
        if (departmentId != null) {
            departmentFilter = departmentRepository.findById(departmentId)
                    .map(Department::getName)
                    .orElse(null);
        }
        final String deptName = departmentFilter;

        List<EmployeePerformanceScore> filtered = results.stream()
                .filter(s -> employeeId == null
                        || (s.getEmployee() != null && employeeId.equals(s.getEmployee().getId())))
                .filter(s -> deptName == null || deptName.equals(s.getDepartmentName()))
                .toList();

        // --------------------------------------------------------
        // WINNER FOR SELECTED MONTH (or latest available)
        // --------------------------------------------------------
        EmployeeOfTheMonth winner = winnerRepository
                .findByEvalYearAndEvalMonth(selYear, selMonth)
                .orElse(null);

        EmployeeOfTheMonth latestWinner = winnerRepository
                .findTopByOrderByEvalYearDescEvalMonthDesc()
                .orElse(null);

        model.addAttribute("results", filtered);
        model.addAttribute("allResults", results);
        model.addAttribute("winner", winner);
        model.addAttribute("latestWinner", latestWinner);
        model.addAttribute("hasResults", !results.isEmpty());
        model.addAttribute("selectedYear", selYear);
        model.addAttribute("selectedMonth", selMonth);
        model.addAttribute("selectedMonthLabel", monthLabel(selYear, selMonth));
        model.addAttribute("months", buildMonthOptions(current));
        model.addAttribute("employees", employeeRepository.findAllByOrderByFirstNameAsc());
        model.addAttribute("departments", departmentRepository.findAvailableDepartments());
        model.addAttribute("selectedEmployeeId", employeeId);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("settings", settingsService.getSettings());

        return "admin/performance/dashboard";
    }

    // ============================================================
    // EMPLOYEE PERFORMANCE DETAIL
    // ============================================================

    @GetMapping("/employee/{employeeId}")
    public String employeeDetail(@PathVariable("employeeId") Long employeeId,
                                 @RequestParam(name = "year", required = false) Integer year,
                                 @RequestParam(name = "month", required = false) Integer month,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        YearMonth current = YearMonth.now();
        int selYear = year != null ? year : current.getYear();
        int selMonth = month != null ? month : current.getMonthValue();

        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/admin/performance";
        }

        EmployeePerformanceScore score = scoringService.getEmployeeScore(employeeId, selYear, selMonth);
        List<PerformanceTaskBreakdown> breakdown = score != null
                ? scoringService.getTaskBreakdown(score.getId())
                : List.of();

        model.addAttribute("employee", employee);
        model.addAttribute("score", score);
        model.addAttribute("breakdown", breakdown);
        model.addAttribute("selectedYear", selYear);
        model.addAttribute("selectedMonth", selMonth);
        model.addAttribute("selectedMonthLabel", monthLabel(selYear, selMonth));
        model.addAttribute("months", buildMonthOptions(current));
        model.addAttribute("settings", settingsService.getSettings());

        return "admin/performance/employee";
    }

    // ============================================================
    // SETTINGS
    // ============================================================

    @GetMapping("/settings")
    public String settings(Model model) {
        if (!model.containsAttribute("settings")) {
            model.addAttribute("settings", settingsService.getSettings());
        }
        return "admin/performance/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute("settings") PerformanceSettings form,
                               RedirectAttributes redirectAttributes) {
        try {
            settingsService.saveSettings(form);
            redirectAttributes.addFlashAttribute("successMessage", "Performance settings saved.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("settings", form);
        }
        return "redirect:/admin/performance/settings";
    }

    // ============================================================
    // MANUAL EVALUATION
    // ============================================================

    @PostMapping("/evaluate")
    public String evaluate(@RequestParam("year") int year,
                           @RequestParam("month") int month,
                           RedirectAttributes redirectAttributes) {
        try {
            EvaluationResult result = scoringService.evaluateMonth(year, month);
            String message = result.winnerSelected()
                    ? "Evaluation complete for " + monthLabel(year, month) + ". Employee of the Month: "
                            + result.winnerName() + " (" + result.winnerScore() + "/100)."
                    : "Evaluation complete for " + monthLabel(year, month)
                            + ". No eligible employees found.";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Evaluation failed: " + e.getMessage());
        }
        return "redirect:/admin/performance?year=" + year + "&month=" + month;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private List<MonthOption> buildMonthOptions(YearMonth current) {
        List<MonthOption> options = new ArrayList<>();
        for (int i = 0; i < MONTH_HISTORY; i++) {
            YearMonth ym = current.minusMonths(i);
            boolean hasResults = scoreRepository.existsByEvalYearAndEvalMonth(
                    ym.getYear(), ym.getMonthValue());
            options.add(new MonthOption(
                    ym.getYear(),
                    ym.getMonthValue(),
                    monthLabel(ym.getYear(), ym.getMonthValue()),
                    hasResults));
        }
        return options;
    }

    private String monthLabel(int year, int month) {
        String name = YearMonth.of(year, month).getMonth().name();
        return name.charAt(0) + name.substring(1).toLowerCase() + " " + year;
    }
}
