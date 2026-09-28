package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.UserRepository;
import com.aslenix.attendance.service.EmployeeDeletionService;
import com.aslenix.attendance.service.FileUploadService;
import com.aslenix.attendance.service.QrCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final QrCodeService qrCodeService;
    private final FileUploadService fileUploadService;
    private final EmployeeDeletionService employeeDeletionService;
    private final com.aslenix.attendance.service.EmployeeCodeService employeeCodeService;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            QrCodeService qrCodeService,
            FileUploadService fileUploadService,
            EmployeeDeletionService employeeDeletionService,
            com.aslenix.attendance.service.EmployeeCodeService employeeCodeService) {

        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.qrCodeService = qrCodeService;
        this.fileUploadService = fileUploadService;
        this.employeeDeletionService = employeeDeletionService;
        this.employeeCodeService = employeeCodeService;
    }

    // =========================
    // EMPLOYEE LIST
    // =========================

    @GetMapping
    public String employees(Model model) {

        model.addAttribute(
                "employees",
                employeeRepository.findAll()
        );

        model.addAttribute(
                "departments",
                departmentRepository.findAll()
        );

        return "admin/employees";
    }

    // =========================
    // EMPLOYEE QR IMAGE
    // =========================

    @GetMapping("/{id}/qr")
    @ResponseBody
    public ResponseEntity<byte[]> employeeQrImage(@PathVariable Long id) {

        Employee employee = employeeRepository.findById(id).orElse(null);

        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        String qrToken = qrCodeService.ensureQrToken(employee);

        byte[] qrImage = qrCodeService.generateQrImage(
                qrToken,
                300,
                300
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    // =========================
    // ADD EMPLOYEE FORM
    // =========================

    @GetMapping("/add")
    public String addEmployeeForm(Model model) {

        model.addAttribute("employee", new Employee());

        model.addAttribute(
                "departments",
                departmentRepository.findAll()
        );

        model.addAttribute(
                "nextEmployeeNumber",
                employeeCodeService.peekNextEmployeeNumber()
        );

        return "admin/add-employee";
    }

    // ============================================================
    // API: PREVIEW NEXT CODE BY DEPARTMENT
    // ============================================================

    @GetMapping("/api/next-code")
    @ResponseBody
    public ResponseEntity<?> getNextCode(@RequestParam Long departmentId) {
        Department dept = departmentRepository.findById(departmentId).orElse(null);
        if (dept == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Department not found"));
        }
        return ResponseEntity.ok(java.util.Map.of(
                "employeeCode", employeeCodeService.previewNextEmployeeCode(dept),
                "abbreviation", dept.getEffectiveAbbreviation(),
                "nextNumber", employeeCodeService.peekNextEmployeeNumber()
        ));
    }

    // =========================
    // SAVE EMPLOYEE
    // =========================

    @PostMapping("/add")
    public String addEmployee(
            @ModelAttribute Employee employee,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam String password,
            @RequestParam Long departmentId,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile) {

        // =========================================================
        // CHECK EMAIL
        // =========================================================

        if (employee.getEmail() != null
                && employeeRepository.findByEmail(employee.getEmail()).isPresent()) {

            return "redirect:/admin/employees/add?error=email";
        }

        // =========================================================
        // FIND DEPARTMENT
        // =========================================================

        Department department = departmentRepository
                .findById(departmentId)
                .orElseThrow();

        // =========================================================
        // AUTOMATICALLY GENERATE UNIQUE EMPLOYEE ID & USERNAME
        // Format: DEPARTMENT_ABBREVIATION-NUMBER (e.g. FE-1001)
        // Employee ID and Username are exactly the same.
        // =========================================================

        String generatedCode = employeeCodeService.generateNextEmployeeCode(department);
        employee.setEmployeeCode(generatedCode);

        // =========================================================
        // AUTOMATICALLY GENERATE UNIQUE QR TOKEN
        // =========================================================

        employee.setQrToken(generateQrToken());

        // =========================================================
        // CREATE EMPLOYEE USER ACCOUNT
        // =========================================================

        User user = new User();

        user.setUsername(generatedCode);
        user.setPassword(
                passwordEncoder.encode(password)
        );
        user.setRole(Role.EMPLOYEE);
        user.setEnabled(true);
        user.setPasswordChangeRequired(true);

        userRepository.save(user);

        // =========================================================
        // CONNECT EMPLOYEE WITH USER
        // =========================================================

        employee.setUser(user);

        // =========================================================
        // CONNECT EMPLOYEE WITH DEPARTMENT
        // =========================================================

        employee.setDepartment(department);

        // =========================================================
        // SET JOINING DATE
        // =========================================================

        if (employee.getJoiningDate() == null) {
            employee.setJoiningDate(LocalDate.now());
        }

        // =========================================================
        // ENABLE EMPLOYEE
        // =========================================================

        employee.setEnabled(true);

        // =========================================================
        // OPTIONAL PROFILE PHOTO
        // =========================================================

        if (photoFile != null && !photoFile.isEmpty()) {
            try {
                String photoUrl = fileUploadService.storeEmployeePhoto(photoFile);
                employee.setPhotoUrl(photoUrl);
            } catch (Exception e) {
                // Log and continue gracefully so employee creation isn't blocked
            }
        }

        // =========================================================
        // SAVE EMPLOYEE
        // =========================================================

        employeeRepository.save(employee);

        return "redirect:/admin/employees";
    }

    // ============================================================
    // GENERATE UNIQUE EMPLOYEE CODE
    // ============================================================

    private String generateEmployeeCode(Department department) {
        return employeeCodeService.generateNextEmployeeCode(department);
    }

    // ============================================================
    // GENERATE UNIQUE QR TOKEN
    // ============================================================

    private String generateQrToken() {

        String qrToken;

        do {
            qrToken = UUID.randomUUID().toString();

        } while (
                employeeRepository.findByQrToken(qrToken).isPresent()
        );

        return qrToken;
    }

    // =========================
    // EDIT EMPLOYEE
    // =========================

    @GetMapping("/edit/{id}")
    public String editEmployee(
            @PathVariable Long id,
            Model model) {

        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow();

        model.addAttribute("employee", employee);

        model.addAttribute(
                "departments",
                departmentRepository.findAll()
        );

        return "admin/edit-employee";
    }

    // =========================
    // UPDATE EMPLOYEE
    // =========================

    @PostMapping("/edit/{id}")
    public String updateEmployee(
            @PathVariable Long id,
            @ModelAttribute Employee employee,
            @RequestParam Long departmentId,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile,
            @RequestParam(value = "removePhoto", required = false, defaultValue = "false") boolean removePhoto,
            RedirectAttributes redirectAttributes) {

        Employee existing = employeeRepository
                .findById(id)
                .orElseThrow();

        // =========================================================
        // EMPLOYEE CODE (IMMUTABLE)
        // System-generated employee ID and login username cannot be
        // manually modified by admin. Existing code is preserved.
        // =========================================================

        // =========================================================
        // EMAIL
        // =========================================================

        if (employee.getEmail() != null
                && !employee.getEmail().equalsIgnoreCase(existing.getEmail())) {

            if (employeeRepository.findByEmail(
                    employee.getEmail()).isPresent()) {

                return "redirect:/admin/employees/edit/"
                        + id
                        + "?error=email";
            }

            existing.setEmail(
                    employee.getEmail()
            );
        }

        // =========================================================
        // PASSWORD UPDATE (IF PROVIDED)
        // =========================================================
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.trim().length() < 6) {
                return "redirect:/admin/employees/edit/"
                        + id
                        + "?error=password_length";
            }
            User user = existing.getUser();
            if (user == null) {
                String username = (existing.getEmail() != null && !existing.getEmail().isBlank())
                        ? existing.getEmail()
                        : existing.getEmployeeCode().toLowerCase();
                if (userRepository.existsByUsername(username)) {
                    username = existing.getEmployeeCode().toLowerCase();
                }
                user = new User();
                user.setUsername(username);
                user.setRole(Role.EMPLOYEE);
                user.setEnabled(existing.isEnabled());
                user.setPasswordChangeRequired(true);
                user = userRepository.save(user);
                existing.setUser(user);
            }
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
            user.setPasswordChangeRequired(true);
            userRepository.save(user);
        }

        // =========================================================
        // BASIC INFORMATION
        // =========================================================

        existing.setFirstName(
                employee.getFirstName()
        );

        existing.setLastName(
                employee.getLastName()
        );

        existing.setPhone(
                employee.getPhone()
        );

        existing.setPosition(
                employee.getPosition()
        );

        existing.setJoiningDate(
                employee.getJoiningDate()
        );

        // =========================================================
        // DEPARTMENT
        // =========================================================

        Department department = departmentRepository
                .findById(departmentId)
                .orElseThrow();

        existing.setDepartment(department);

        // =========================================================
        // PROFILE PHOTO
        // =========================================================

        if (removePhoto) {
            if (existing.getPhotoUrl() != null) {
                fileUploadService.deleteEmployeePhoto(existing.getPhotoUrl());
                existing.setPhotoUrl(null);
            }
        } else if (photoFile != null && !photoFile.isEmpty()) {
            try {
                if (existing.getPhotoUrl() != null) {
                    fileUploadService.deleteEmployeePhoto(existing.getPhotoUrl());
                }
                String photoUrl = fileUploadService.storeEmployeePhoto(photoFile);
                existing.setPhotoUrl(photoUrl);
            } catch (Exception e) {
                // Log and continue gracefully
            }
        }

        // =========================================================
        // SAVE
        // =========================================================

        employeeRepository.save(existing);

        String employeeName = existing.getFirstName() + (existing.getLastName() != null ? " " + existing.getLastName() : "");
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Employee " + employeeName + " updated successfully."
        );

        return "redirect:/admin/employees";
    }

    // =========================
    // RESET EMPLOYEE PASSWORD
    // =========================

    @PostMapping("/reset-password/{id}")
    public String resetEmployeePassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes) {

        Employee employee = employeeRepository.findById(id).orElse(null);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/admin/employees";
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters long.");
            return "redirect:/admin/employees";
        }

        User user = employee.getUser();
        if (user == null) {
            String username = (employee.getEmail() != null && !employee.getEmail().isBlank())
                    ? employee.getEmail()
                    : employee.getEmployeeCode().toLowerCase();
            if (userRepository.existsByUsername(username)) {
                username = employee.getEmployeeCode().toLowerCase();
            }
            user = new User();
            user.setUsername(username);
            user.setRole(Role.EMPLOYEE);
            user.setEnabled(employee.isEnabled());
            user.setPasswordChangeRequired(true);
            user = userRepository.save(user);
            employee.setUser(user);
            employeeRepository.save(employee);
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);

        String employeeName = employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "");
        redirectAttributes.addFlashAttribute("successMessage",
                "Password successfully reset for " + employeeName + ".");
        return "redirect:/admin/employees";
    }

    // =========================
    // DISABLE EMPLOYEE
    // =========================

    @PostMapping("/disable/{id}")
    public String disableEmployee(
            @PathVariable Long id) {

        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow();

        employee.setEnabled(false);

        if (employee.getUser() != null) {

            employee.getUser().setEnabled(false);

            userRepository.save(
                    employee.getUser()
            );
        }

        employeeRepository.save(employee);

        return "redirect:/admin/employees";
    }

    // =========================
    // ENABLE EMPLOYEE
    // =========================

    @PostMapping("/enable/{id}")
    public String enableEmployee(
            @PathVariable Long id) {

        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow();

        employee.setEnabled(true);

        if (employee.getUser() != null) {

            employee.getUser().setEnabled(true);

            userRepository.save(
                    employee.getUser()
            );
        }

        employeeRepository.save(employee);

        return "redirect:/admin/employees";
    }

    // =========================
    // DELETE EMPLOYEE
    // =========================

    @PostMapping("/delete/{id}")
    public String deleteEmployee(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            boolean deleted = employeeDeletionService.deleteEmployee(id);

            if (deleted) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Employee deleted permanently."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Employee not found."
                );
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to delete employee: " + (e.getMessage() != null ? e.getMessage() : "Unexpected error")
            );
        }

        return "redirect:/admin/employees";
    }
}