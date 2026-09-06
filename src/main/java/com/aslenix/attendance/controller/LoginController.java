package com.aslenix.attendance.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // ============================================================
    // LOGIN PAGE
    // ============================================================

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // ============================================================
    // ROOT ROUTE
    //
    // After login, Spring sends the user here.
    // We then send ADMIN and EMPLOYEE to their own dashboards.
    // ============================================================

    @GetMapping("/")
    public String home(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return "redirect:/login";
        }

        // --------------------------------------------------------
        // ADMIN
        // --------------------------------------------------------

        if (authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()))) {

            return "redirect:/admin/dashboard";
        }

        // --------------------------------------------------------
        // EMPLOYEE
        // --------------------------------------------------------

        if (authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_EMPLOYEE".equals(
                                authority.getAuthority()))) {

            return "redirect:/employee/dashboard";
        }

        // --------------------------------------------------------
        // Unknown role
        // --------------------------------------------------------

        return "redirect:/login?error";
    }
}