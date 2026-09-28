package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.OfficeSettings;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.OfficeSettingsRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Controller
@RequestMapping("/setup")
public class SetupController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficeSettingsRepository officeSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupController(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            OfficeSettingsRepository officeSettingsRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.officeSettingsRepository = officeSettingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Show initial system setup page if no ADMIN user exists.
     * If an admin user already exists, immediately lock down and redirect to login.
     */
    @GetMapping
    public String showSetupPage(Model model) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return "redirect:/login";
        }
        return "setup";
    }

    /**
     * Process initial admin creation.
     */
    @PostMapping
    public String processSetup(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "departmentName", required = false) String departmentName,
            RedirectAttributes redirectAttributes) {

        // Strict lockdown: if an admin already exists, do not permit re-initialization
        if (userRepository.existsByRole(Role.ADMIN)) {
            return "redirect:/login";
        }

        // --- VALIDATIONS ---
        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Administrator username is required.");
            populateFormRetain(redirectAttributes, username, fullName, email, departmentName);
            return "redirect:/setup";
        }

        String cleanUsername = username.trim();
        if (cleanUsername.length() < 3) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username must be at least 3 characters long.");
            populateFormRetain(redirectAttributes, username, fullName, email, departmentName);
            return "redirect:/setup";
        }

        if (userRepository.existsByUsername(cleanUsername)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username '" + cleanUsername + "' is already taken.");
            populateFormRetain(redirectAttributes, username, fullName, email, departmentName);
            return "redirect:/setup";
        }

        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters long.");
            populateFormRetain(redirectAttributes, username, fullName, email, departmentName);
            return "redirect:/setup";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match. Please verify.");
            populateFormRetain(redirectAttributes, username, fullName, email, departmentName);
            return "redirect:/setup";
        }

        // --- CREATE SUPER ADMIN USER ---
        User admin = new User();
        admin.setUsername(cleanUsername);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setPasswordChangeRequired(false);
        admin = userRepository.save(admin);

        // --- ENSURE DEFAULT OFFICE SETTINGS ---
        if (officeSettingsRepository.count() == 0) {
            OfficeSettings settings = new OfficeSettings();
            settings.setOfficeName("ASLENIX");
            settings.setOfficeLocation("ASLENIX Headquarters");
            settings.setLatitude(27.68777628544264);
            settings.setLongitude(85.3303680512975);
            settings.setAllowedRadiusMeters(100.0);
            settings.setWorkStartTime(LocalTime.of(10, 0));
            settings.setWorkEndTime(LocalTime.of(18, 0));
            settings.setLateGraceMinutes(15);
            settings.setSunday(true);
            settings.setMonday(true);
            settings.setTuesday(true);
            settings.setWednesday(true);
            settings.setThursday(true);
            settings.setFriday(true);
            settings.setSaturday(false);
            officeSettingsRepository.save(settings);
        }

        // --- ENSURE DEPARTMENT ---
        String deptName = (departmentName != null && !departmentName.trim().isEmpty())
                ? departmentName.trim()
                : "Administration";
        Department department = departmentRepository.findByName(deptName).orElseGet(() -> {
            Department d = new Department();
            d.setName(deptName);
            d.setActive(true);
            return departmentRepository.save(d);
        });

        // --- CREATE LINKED ADMIN EMPLOYEE PROFILE ---
        String name = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : "System Administrator";
        String[] parts = name.split("\\s+", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "Administrator";

        String adminEmail = (email != null && !email.trim().isEmpty())
                ? email.trim()
                : (cleanUsername + "@aslenix.local");

        if (employeeRepository.findByEmail(adminEmail).isEmpty()) {
            Employee adminEmp = new Employee();
            adminEmp.setUser(admin);
            adminEmp.setFirstName(firstName);
            adminEmp.setLastName(lastName);
            adminEmp.setEmail(adminEmail);
            adminEmp.setEmployeeCode("ADM-001");
            adminEmp.setQrToken(UUID.randomUUID().toString());
            adminEmp.setPosition("Super Administrator");
            adminEmp.setJoiningDate(LocalDate.now());
            adminEmp.setEnabled(true);
            adminEmp.setDepartment(department);
            employeeRepository.save(adminEmp);
        }

        return "redirect:/login?setup=success";
    }

    private void populateFormRetain(
            RedirectAttributes redirectAttributes,
            String username,
            String fullName,
            String email,
            String departmentName) {
        redirectAttributes.addFlashAttribute("retainedUsername", username);
        redirectAttributes.addFlashAttribute("retainedFullName", fullName);
        redirectAttributes.addFlashAttribute("retainedEmail", email);
        redirectAttributes.addFlashAttribute("retainedDepartmentName", departmentName);
    }
}
