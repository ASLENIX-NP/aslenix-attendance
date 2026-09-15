package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.entity.WeeklyWorkingSchedule;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.repository.WeeklyWorkingScheduleRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
public class OfficeSettingsController {

    private final OfficeSettingsRepository officeSettingsRepository;
    private final WeeklyWorkingScheduleRepository weeklyWorkingScheduleRepository;

    private static final LocalTime DEFAULT_START_TIME =
            LocalTime.of(10, 0);

    private static final LocalTime DEFAULT_END_TIME =
            LocalTime.of(18, 0);

    private static final int DEFAULT_LATE_GRACE_MINUTES = 15;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public OfficeSettingsController(
            OfficeSettingsRepository officeSettingsRepository,
            WeeklyWorkingScheduleRepository weeklyWorkingScheduleRepository) {

        this.officeSettingsRepository = officeSettingsRepository;
        this.weeklyWorkingScheduleRepository =
                weeklyWorkingScheduleRepository;
    }


    // ============================================================
    // SETTINGS PAGE
    // ============================================================

    @GetMapping
    public String settings(Model model) {

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElseGet(() -> {

                            OfficeSettings newSettings =
                                    new OfficeSettings();

                            // ------------------------------------------------
                            // OFFICE
                            // ------------------------------------------------

                            newSettings.setOfficeName("ASLENIX");

                            newSettings.setOfficeLocation(
                                    "ASLENIX Office"
                            );

                            // ------------------------------------------------
                            // LOCATION
                            // ------------------------------------------------

                            newSettings.setLatitude(
                                    27.68777628544264
                            );

                            newSettings.setLongitude(
                                    85.3303680512975
                            );

                            newSettings.setAllowedRadiusMeters(
                                    500.0
                            );

                            // ------------------------------------------------
                            // WORKING HOURS
                            // ------------------------------------------------

                            newSettings.setWorkStartTime(
                                    DEFAULT_START_TIME
                            );

                            newSettings.setWorkEndTime(
                                    DEFAULT_END_TIME
                            );

                            newSettings.setLateGraceMinutes(
                                    DEFAULT_LATE_GRACE_MINUTES
                            );

                            // ------------------------------------------------
                            // WORKING DAYS
                            //
                            // Sunday-Friday = working
                            // Saturday = holiday
                            // ------------------------------------------------

                            newSettings.setSunday(true);
                            newSettings.setMonday(true);
                            newSettings.setTuesday(true);
                            newSettings.setWednesday(true);
                            newSettings.setThursday(true);
                            newSettings.setFriday(true);
                            newSettings.setSaturday(false);

                            return officeSettingsRepository.save(
                                    newSettings
                            );
                        });


        // ============================================================
        // SAFETY FOR OLD DATABASE RECORDS
        // ============================================================

        boolean changed = false;

        if (settings.getWorkStartTime() == null) {

            settings.setWorkStartTime(
                    DEFAULT_START_TIME
            );

            changed = true;
        }

        if (settings.getWorkEndTime() == null) {

            settings.setWorkEndTime(
                    DEFAULT_END_TIME
            );

            changed = true;
        }

        if (changed) {

            settings =
                    officeSettingsRepository.save(settings);
        }


        // ============================================================
        // CURRENT WEEK
        //
        // Sunday -> Saturday
        // ============================================================

        LocalDate today = LocalDate.now();

        LocalDate weekStart =
                today.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.SUNDAY
                        )
                );

        LocalDate weekEnd =
                weekStart.plusDays(6);


        // ============================================================
        // GET CURRENT WEEK SCHEDULE
        // ============================================================

        List<WeeklyWorkingSchedule> weeklySchedule =
                weeklyWorkingScheduleRepository
                        .findByWeekStartOrderByWorkDateAsc(
                                weekStart
                        );


        // ============================================================
        // CREATE CURRENT WEEK IF MISSING
        // ============================================================

        if (weeklySchedule.isEmpty()) {

            weeklySchedule =
                    createWeeklySchedule(
                            weekStart,
                            settings
                    );
        }


        // ============================================================
        // SEND DATA TO THYMELEAF
        // ============================================================

        model.addAttribute(
                "settings",
                settings
        );

        model.addAttribute(
                "weeklySchedule",
                weeklySchedule
        );

        model.addAttribute(
                "weekStart",
                weekStart
        );

        model.addAttribute(
                "weekEnd",
                weekEnd
        );


        return "admin/settings";
    }


    // ============================================================
    // SAVE SETTINGS
    // ============================================================

    @PostMapping
    public String saveSettings(

            @ModelAttribute("settings")
            OfficeSettings formSettings,

            @RequestParam(
                    name = "workStartTime",
                    required = false
            )
            String workStartTime,

            @RequestParam(
                    name = "workEndTime",
                    required = false
            )
            String workEndTime,

            @RequestParam(
                    name = "workingDates",
                    required = false
            )
            List<String> workingDates,

            Model model) {


        // ============================================================
        // GET EXISTING SETTINGS
        // ============================================================

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElseGet(
                                OfficeSettings::new
                        );


        // ============================================================
        // OFFICE NAME
        // ============================================================

        if (formSettings.getOfficeName() != null
                && !formSettings.getOfficeName()
                .trim()
                .isEmpty()) {

            settings.setOfficeName(
                    formSettings.getOfficeName().trim()
            );
        }


        // ============================================================
        // OFFICE LOCATION
        // ============================================================

        if (formSettings.getOfficeLocation() != null
                && !formSettings.getOfficeLocation()
                .trim()
                .isEmpty()) {

            settings.setOfficeLocation(
                    formSettings.getOfficeLocation().trim()
            );
        }


        // ============================================================
        // GPS
        // ============================================================

        settings.setLatitude(
                formSettings.getLatitude()
        );

        settings.setLongitude(
                formSettings.getLongitude()
        );

        settings.setAllowedRadiusMeters(
                formSettings.getAllowedRadiusMeters()
        );


        // ============================================================
        // WORK START TIME
        //
        // IMPORTANT:
        // Read the raw HTML time value and explicitly convert it.
        //
        // Example:
        // "09:30" -> LocalTime 09:30
        //
        // If nothing is submitted, keep the existing database value.
        // ============================================================

        if (workStartTime != null
                && !workStartTime.trim().isEmpty()) {

            try {

                LocalTime parsedStartTime =
                        LocalTime.parse(
                                workStartTime.trim(),
                                TIME_FORMATTER
                        );

                settings.setWorkStartTime(
                        parsedStartTime
                );

            } catch (DateTimeParseException e) {

                model.addAttribute(
                        "errorMessage",
                        "Invalid work start time."
                );

                return loadSettingsWithError(
                        model,
                        settings
                );
            }
        }


        // ============================================================
        // WORK END TIME
        // ============================================================

        if (workEndTime != null
                && !workEndTime.trim().isEmpty()) {

            try {

                LocalTime parsedEndTime =
                        LocalTime.parse(
                                workEndTime.trim(),
                                TIME_FORMATTER
                        );

                settings.setWorkEndTime(
                        parsedEndTime
                );

            } catch (DateTimeParseException e) {

                model.addAttribute(
                        "errorMessage",
                        "Invalid work end time."
                );

                return loadSettingsWithError(
                        model,
                        settings
                );
            }
        }


        // ============================================================
        // FINAL TIME SAFETY
        // ============================================================

        if (settings.getWorkStartTime() == null) {

            settings.setWorkStartTime(
                    DEFAULT_START_TIME
            );
        }

        if (settings.getWorkEndTime() == null) {

            settings.setWorkEndTime(
                    DEFAULT_END_TIME
            );
        }


        // ============================================================
        // LATE GRACE PERIOD
        // ============================================================

        if (formSettings.getLateGraceMinutes() < 0) {

            settings.setLateGraceMinutes(
                    DEFAULT_LATE_GRACE_MINUTES
            );

        } else {

            settings.setLateGraceMinutes(
                    formSettings.getLateGraceMinutes()
            );
        }


        // ============================================================
        // WORKING DAYS
        //
        // The checkboxes use th:field in settings.html.
        //
        // Therefore the boolean values received in formSettings
        // represent the user's current selections.
        // ============================================================

        settings.setSunday(
                formSettings.isSunday()
        );

        settings.setMonday(
                formSettings.isMonday()
        );

        settings.setTuesday(
                formSettings.isTuesday()
        );

        settings.setWednesday(
                formSettings.isWednesday()
        );

        settings.setThursday(
                formSettings.isThursday()
        );

        settings.setFriday(
                formSettings.isFriday()
        );

        settings.setSaturday(
                formSettings.isSaturday()
        );


        // ============================================================
        // SAVE OFFICE SETTINGS
        // ============================================================

        settings =
                officeSettingsRepository.save(
                        settings
                );


        // ============================================================
        // CURRENT WEEK
        // ============================================================

        LocalDate today = LocalDate.now();

        LocalDate weekStart =
                today.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.SUNDAY
                        )
                );


        // ============================================================
        // GET CURRENT WEEK
        // ============================================================

        List<WeeklyWorkingSchedule> weeklySchedule =
                weeklyWorkingScheduleRepository
                        .findByWeekStartOrderByWorkDateAsc(
                                weekStart
                        );


        // ============================================================
        // CREATE WEEK IF NECESSARY
        // ============================================================

        if (weeklySchedule.isEmpty()) {

            weeklySchedule =
                    createWeeklySchedule(
                            weekStart,
                            settings
                    );

        } else {

            // ========================================================
            // UPDATE EXISTING WEEK
            // ========================================================

            for (WeeklyWorkingSchedule schedule :
                    weeklySchedule) {

                DayOfWeek day =
                        schedule
                                .getWorkDate()
                                .getDayOfWeek();

                boolean workingDay =
                        isWorkingDay(
                                settings,
                                day
                        );

                schedule.setWorkingDay(
                        workingDay
                );

                weeklyWorkingScheduleRepository.save(
                        schedule
                );
            }
        }


        // ============================================================
        // REDIRECT AFTER SUCCESSFUL SAVE
        // ============================================================

        return "redirect:/admin/settings?saved=true";
    }


    // ============================================================
    // TOGGLE SPECIFIC DAY IN CURRENT WEEK SCHEDULE (AJAX)
    // ============================================================

    @PostMapping("/schedule/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleScheduleDay(
            @RequestParam("date") String dateStr) {

        try {
            LocalDate date = LocalDate.parse(dateStr.trim());
            LocalDate weekStart = date.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)
            );

            OfficeSettings settings = officeSettingsRepository
                    .findFirstByOrderByIdAsc()
                    .orElseGet(OfficeSettings::new);

            WeeklyWorkingSchedule schedule = weeklyWorkingScheduleRepository
                    .findByWeekStartAndWorkDate(weekStart, date)
                    .orElseGet(() -> new WeeklyWorkingSchedule(
                            weekStart,
                            date,
                            isWorkingDay(settings, date.getDayOfWeek())
                    ));

            schedule.setWorkingDay(!schedule.isWorkingDay());
            weeklyWorkingScheduleRepository.save(schedule);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "date", date.toString(),
                            "workingDay", schedule.isWorkingDay(),
                            "dayName", date.getDayOfWeek().name(),
                            "message", "Schedule updated for " + date.getDayOfWeek().name()
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Failed to update schedule: " + e.getMessage()
                    )
            );
        }
    }


    // ============================================================
    // UPDATE WORKING DAYS DIRECTLY (AJAX)
    // ============================================================

    @PostMapping("/working-days/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateWorkingDays(
            @RequestParam(name = "sunday", defaultValue = "false") boolean sunday,
            @RequestParam(name = "monday", defaultValue = "false") boolean monday,
            @RequestParam(name = "tuesday", defaultValue = "false") boolean tuesday,
            @RequestParam(name = "wednesday", defaultValue = "false") boolean wednesday,
            @RequestParam(name = "thursday", defaultValue = "false") boolean thursday,
            @RequestParam(name = "friday", defaultValue = "false") boolean friday,
            @RequestParam(name = "saturday", defaultValue = "false") boolean saturday) {

        try {
            OfficeSettings settings = officeSettingsRepository
                    .findFirstByOrderByIdAsc()
                    .orElseGet(OfficeSettings::new);

            settings.setSunday(sunday);
            settings.setMonday(monday);
            settings.setTuesday(tuesday);
            settings.setWednesday(wednesday);
            settings.setThursday(thursday);
            settings.setFriday(friday);
            settings.setSaturday(saturday);

            settings = officeSettingsRepository.save(settings);

            // Synchronize current week
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)
            );

            List<WeeklyWorkingSchedule> weeklySchedule =
                    weeklyWorkingScheduleRepository.findByWeekStartOrderByWorkDateAsc(weekStart);

            if (weeklySchedule.isEmpty()) {
                createWeeklySchedule(weekStart, settings);
            } else {
                for (WeeklyWorkingSchedule s : weeklySchedule) {
                    s.setWorkingDay(isWorkingDay(settings, s.getWorkDate().getDayOfWeek()));
                    weeklyWorkingScheduleRepository.save(s);
                }
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Working days updated successfully."
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Failed to update working days: " + e.getMessage()
                    )
            );
        }
    }


    // ============================================================
    // LOAD SETTINGS WITH ERROR
    // ============================================================

    private String loadSettingsWithError(
            Model model,
            OfficeSettings settings) {


        LocalDate today = LocalDate.now();

        LocalDate weekStart =
                today.with(
                        TemporalAdjusters.previousOrSame(
                                DayOfWeek.SUNDAY
                        )
                );

        LocalDate weekEnd =
                weekStart.plusDays(6);


        List<WeeklyWorkingSchedule> weeklySchedule =
                weeklyWorkingScheduleRepository
                        .findByWeekStartOrderByWorkDateAsc(
                                weekStart
                        );


        if (weeklySchedule.isEmpty()) {

            weeklySchedule =
                    createWeeklySchedule(
                            weekStart,
                            settings
                    );
        }


        model.addAttribute(
                "settings",
                settings
        );

        model.addAttribute(
                "weeklySchedule",
                weeklySchedule
        );

        model.addAttribute(
                "weekStart",
                weekStart
        );

        model.addAttribute(
                "weekEnd",
                weekEnd
        );


        return "admin/settings";
    }


    // ============================================================
    // CREATE WEEKLY SCHEDULE
    // ============================================================

    private List<WeeklyWorkingSchedule> createWeeklySchedule(
            LocalDate weekStart,
            OfficeSettings settings) {

        for (int i = 0; i < 7; i++) {

            LocalDate date =
                    weekStart.plusDays(i);

            DayOfWeek day =
                    date.getDayOfWeek();

            boolean workingDay =
                    isWorkingDay(
                            settings,
                            day
                    );

            WeeklyWorkingSchedule schedule =
                    new WeeklyWorkingSchedule(
                            weekStart,
                            date,
                            workingDay
                    );

            weeklyWorkingScheduleRepository.save(
                    schedule
            );
        }


        return weeklyWorkingScheduleRepository
                .findByWeekStartOrderByWorkDateAsc(
                        weekStart
                );
    }


    // ============================================================
    // CHECK WORKING DAY
    // ============================================================

    private boolean isWorkingDay(
            OfficeSettings settings,
            DayOfWeek day) {

        switch (day) {

            case SUNDAY:
                return settings.isSunday();

            case MONDAY:
                return settings.isMonday();

            case TUESDAY:
                return settings.isTuesday();

            case WEDNESDAY:
                return settings.isWednesday();

            case THURSDAY:
                return settings.isThursday();

            case FRIDAY:
                return settings.isFriday();

            case SATURDAY:
                return settings.isSaturday();

            default:
                return false;
        }
    }
}