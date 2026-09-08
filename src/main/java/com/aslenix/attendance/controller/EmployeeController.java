package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Department;
import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.entity.Role;
import com.aslenix.attendance.entity.User;
import com.aslenix.attendance.repository.DepartmentRepository;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
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

        return "admin/employees";
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

        return "admin/add-employee";
    }

    // =========================
    // SAVE EMPLOYEE
    // =========================

    @PostMapping("/add")
    public String addEmployee(
            @ModelAttribute Employee employee,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Long departmentId) {

        // =========================================================
        // CHECK USERNAME
        // =========================================================

        if (userRepository.existsByUsername(username)) {

            return "redirect:/admin/employees/add?error=username";
        }

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
        // AUTOMATICALLY GENERATE UNIQUE EMPLOYEE CODE
        // =========================================================

        employee.setEmployeeCode(generateEmployeeCode());

        // =========================================================
        // AUTOMATICALLY GENERATE UNIQUE QR TOKEN
        // =========================================================

        employee.setQrToken(generateQrToken());

        // =========================================================
        // CREATE EMPLOYEE USER ACCOUNT
        // =========================================================

        User user = new User();

        user.setUsername(username);
        user.setPassword(
                passwordEncoder.encode(password)
        );
        user.setRole(Role.EMPLOYEE);
        user.setEnabled(true);
        user.setPasswordChangeRequired(false);

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
        // SAVE EMPLOYEE
        // =========================================================

        employeeRepository.save(employee);

        return "redirect:/admin/employees";
    }

    // ============================================================
    // GENERATE UNIQUE EMPLOYEE CODE
    // ============================================================

    private String generateEmployeeCode() {

        String employeeCode;

        do {
            employeeCode = "ASL-" +
                    String.format(
                            "%05d",
                            (int) (Math.random() * 100000)
                    );

        } while (
                employeeRepository.existsByEmployeeCode(employeeCode)
        );

        return employeeCode;
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
            @RequestParam Long departmentId) {

        Employee existing = employeeRepository
                .findById(id)
                .orElseThrow();

        // =========================================================
        // EMPLOYEE CODE
        // =========================================================

        if (employee.getEmployeeCode() != null
                && !employee.getEmployeeCode().equals(existing.getEmployeeCode())) {

            if (employeeRepository.existsByEmployeeCode(
                    employee.getEmployeeCode())) {

                return "redirect:/admin/employees/edit/"
                        + id
                        + "?error=code";
            }

            existing.setEmployeeCode(
                    employee.getEmployeeCode()
            );
        }

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
        // SAVE
        // =========================================================

        employeeRepository.save(existing);

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
}