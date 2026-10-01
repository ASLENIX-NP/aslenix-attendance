package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.AdminRecoveryToken;
import com.aslenix.attendance.service.AdminRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/recovery")
public class AdminRecoveryController {

    private static final Logger log =
            LoggerFactory.getLogger(AdminRecoveryController.class);

    private final AdminRecoveryService recoveryService;

    public AdminRecoveryController(AdminRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    // ================================================================
    // SHOW RECOVERY FORM
    // ================================================================

    @GetMapping("/{token}")
    public String showRecoveryForm(
            @PathVariable("token") String token,
            Model model) {

        Optional<AdminRecoveryToken> validToken =
                recoveryService.validateToken(token);

        if (validToken.isEmpty()) {
            model.addAttribute("tokenInvalid", true);
            return "admin-recovery";
        }

        model.addAttribute("token", token);
        model.addAttribute("tokenInvalid", false);
        return "admin-recovery";
    }

    // ================================================================
    // PROCESS PASSWORD RESET
    // ================================================================

    @PostMapping("/{token}")
    public String processRecovery(
            @PathVariable("token") String token,
            @RequestParam(value = "password", required = false)
                    String password,
            @RequestParam(value = "confirmPassword", required = false)
                    String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Re-validate token
        Optional<AdminRecoveryToken> validToken =
                recoveryService.validateToken(token);

        if (validToken.isEmpty()) {
            model.addAttribute("tokenInvalid", true);
            return "admin-recovery";
        }

        // Validate passwords match
        if (password == null || password.isBlank()) {
            model.addAttribute("token", token);
            model.addAttribute("tokenInvalid", false);
            model.addAttribute("errorMessage",
                    "Password is required.");
            return "admin-recovery";
        }

        if (password.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("tokenInvalid", false);
            model.addAttribute("errorMessage",
                    "Password must be at least 6 characters long.");
            return "admin-recovery";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("tokenInvalid", false);
            model.addAttribute("errorMessage",
                    "Passwords do not match. Please verify.");
            return "admin-recovery";
        }

        // Attempt password reset
        String error = recoveryService.resetPassword(token, password);

        if (error != null) {
            model.addAttribute("token", token);
            model.addAttribute("tokenInvalid", false);
            model.addAttribute("errorMessage", error);
            return "admin-recovery";
        }

        // Success — redirect to login with message
        redirectAttributes.addFlashAttribute("recoverySuccess", true);
        return "redirect:/login?recovery=success";
    }
}
