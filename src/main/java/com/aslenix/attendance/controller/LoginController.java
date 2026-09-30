package com.aslenix.attendance.controller;
 
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final UserRepository userRepository;

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ============================================================
    // LOGIN PAGE
    // ============================================================

    @GetMapping("/login")
    public String login() {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            return "redirect:/setup";
        }
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

            if (!userRepository.existsByRole(Role.ADMIN)) {
                return "redirect:/setup";
            }
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

            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null && user.isPasswordChangeRequired()) {
                return "redirect:/employee/change-password";
            }

            return "redirect:/employee/attendance";
        }

        // --------------------------------------------------------
        // Unknown role
        // --------------------------------------------------------

        return "redirect:/login?error";
    }
}