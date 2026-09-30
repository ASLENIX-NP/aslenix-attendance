package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/employee")
public class EmployeePasswordController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeePasswordController(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/change-password")
    public String showChangePasswordPage(
            Authentication authentication,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Employee employee = employeeRepository.findByUserUsername(authentication.getName()).orElse(null);
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);

        boolean isFirstLogin = user != null && user.isPasswordChangeRequired();

        model.addAttribute("employee", employee);
        model.addAttribute("employeeName", employee != null
                ? employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "")
                : authentication.getName());
        model.addAttribute("isFirstLogin", isFirstLogin);
        model.addAttribute("today", LocalDate.now());

        return "employee/change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "source", required = false) String source,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "User account not found.");
            return "redirect:/login";
        }

        String redirectTarget = "profile".equalsIgnoreCase(source)
                ? "redirect:/employee/profile"
                : "redirect:/employee/change-password";

        // 1. Current password check
        if (currentPassword == null || currentPassword.isBlank() || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Current password is incorrect.");
            return redirectTarget;
        }

        // 2. Minimum length check
        if (newPassword == null || newPassword.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password must be at least 6 characters long.");
            return redirectTarget;
        }

        // 3. Match confirm password
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirm password do not match.");
            return redirectTarget;
        }

        // 4. Must be different from current password
        if (passwordEncoder.matches(newPassword.trim(), user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password cannot be the same as your current password.");
            return redirectTarget;
        }

        // 5. Save new password & clear requirement
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Password successfully updated.");

        if ("profile".equalsIgnoreCase(source)) {
            return "redirect:/employee/profile";
        } else {
            return "redirect:/employee/attendance";
        }
    }
}
