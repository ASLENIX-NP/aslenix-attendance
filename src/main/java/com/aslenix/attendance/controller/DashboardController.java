package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final OfficeSettingsRepository officeSettingsRepository;

    public DashboardController(
            OfficeSettingsRepository officeSettingsRepository) {

        this.officeSettingsRepository = officeSettingsRepository;
    }

    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        OfficeSettings settings =
                officeSettingsRepository
                        .findFirstByOrderByIdAsc()
                        .orElse(null);

        model.addAttribute("settings", settings);

        return "admin/dashboard";
    }
}