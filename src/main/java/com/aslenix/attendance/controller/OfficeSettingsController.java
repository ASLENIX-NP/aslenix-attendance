package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

@Controller
@RequestMapping("/admin/settings")
public class OfficeSettingsController {

    private final OfficeSettingsRepository officeSettingsRepository;

    public OfficeSettingsController(
            OfficeSettingsRepository officeSettingsRepository) {

        this.officeSettingsRepository = officeSettingsRepository;
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

                            // ====================================================
                            // OFFICE INFORMATION
                            // ====================================================

                            newSettings.setOfficeName("ASLENIX");

                            newSettings.setOfficeLocation(
                                    "ASLENIX Office"
                            );

                            // ====================================================
                            // OFFICE GPS LOCATION
                            // ====================================================

                            newSettings.setLatitude(27.68777628544264);

                            newSettings.setLongitude(85.3303680512975);

                            // Employees can check in within this radius
                            newSettings.setAllowedRadiusMeters(500.0);

                            // ====================================================
                            // WORKING HOURS
                            // ====================================================

                            newSettings.setWorkStartTime(
                                    LocalTime.of(10, 0)
                            );

                            newSettings.setWorkEndTime(
                                    LocalTime.of(18, 0)
                            );

                            // Employees arriving after
                            // 10:15 AM will be considered late.
                            newSettings.setLateGraceMinutes(
                                    15
                            );

                            // ====================================================
                            // WORKING DAYS
                            // ====================================================

                            newSettings.setSunday(false);

                            newSettings.setMonday(true);

                            newSettings.setTuesday(true);

                            newSettings.setWednesday(true);

                            newSettings.setThursday(true);

                            newSettings.setFriday(true);

                            newSettings.setSaturday(false);

                            // ====================================================
                            // SAVE DEFAULT SETTINGS
                            // ====================================================

                            return officeSettingsRepository.save(
                                    newSettings
                            );
                        });

        model.addAttribute(
                "settings",
                settings
        );

        return "admin/settings";
    }


    // ============================================================
    // SAVE SETTINGS
    // ============================================================

    @PostMapping
    public String saveSettings(
            @ModelAttribute("settings")
            OfficeSettings formSettings) {

        // ========================================================
        // GET EXISTING SETTINGS
        // ========================================================

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElseGet(
                                OfficeSettings::new
                        );


        // ========================================================
        // OFFICE INFORMATION
        // ========================================================

        settings.setOfficeName(
                formSettings.getOfficeName()
        );

        settings.setOfficeLocation(
                formSettings.getOfficeLocation()
        );


        // ========================================================
        // GPS SETTINGS
        // ========================================================

        settings.setLatitude(
                formSettings.getLatitude()
        );

        settings.setLongitude(
                formSettings.getLongitude()
        );

        settings.setAllowedRadiusMeters(
                formSettings.getAllowedRadiusMeters()
        );


        // ========================================================
        // WORKING HOURS
        // ========================================================

        if (formSettings.getWorkStartTime() != null) {

            settings.setWorkStartTime(
                    formSettings.getWorkStartTime()
            );

        } else {

            settings.setWorkStartTime(
                    LocalTime.of(10, 0)
            );
        }


        if (formSettings.getWorkEndTime() != null) {

            settings.setWorkEndTime(
                    formSettings.getWorkEndTime()
            );

        } else {

            settings.setWorkEndTime(
                    LocalTime.of(18, 0)
            );
        }


        // ========================================================
        // LATE GRACE PERIOD
        // ========================================================

        settings.setLateGraceMinutes(
                formSettings.getLateGraceMinutes()
        );


        // ========================================================
        // WORKING DAYS
        // ========================================================

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


        // ========================================================
        // SAVE TO DATABASE
        // ========================================================

        officeSettingsRepository.save(
                settings
        );


        // ========================================================
        // RETURN TO SETTINGS PAGE
        // ========================================================

        return "redirect:/admin/settings?saved=true";
    }
}