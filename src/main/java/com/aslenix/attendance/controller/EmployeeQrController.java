package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.QrCodeService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class EmployeeQrController {

    private final EmployeeRepository employeeRepository;

    private final QrCodeService qrCodeService;

    public EmployeeQrController(
            EmployeeRepository employeeRepository,
            QrCodeService qrCodeService) {

        this.employeeRepository =
                employeeRepository;

        this.qrCodeService =
                qrCodeService;
    }

    // ============================================================
    // EMPLOYEE QR PAGE
    // ============================================================

    @GetMapping("/employee/qr")
    public String employeeQr(
            Authentication authentication,
            Model model) {

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {

            return "redirect:/login";
        }

        // Make sure the employee has a QR token

        qrCodeService.ensureQrToken(employee);

        String fullName =
                employee.getFirstName()
                        + " "
                        + employee.getLastName();

        String qrUrl =
                "/verify-employee/"
                        + employee.getQrToken();

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "employeeName",
                fullName
        );

        model.addAttribute(
                "qrToken",
                employee.getQrToken()
        );

        model.addAttribute(
                "qrUrl",
                qrUrl
        );

        return "employee/qr";
    }

    // ============================================================
    // ADMIN VIEW EMPLOYEE QR
    // ============================================================

    @GetMapping("/admin/employee-qr/{id}")
    public String adminEmployeeQr(
            @PathVariable Long id,
            Model model) {

        Optional<Employee> result =
                employeeRepository.findById(id);

        if (result.isEmpty()) {

            return "redirect:/admin/employees";
        }

        Employee employee =
                result.get();

        qrCodeService.ensureQrToken(employee);

        String fullName =
                employee.getFirstName()
                        + " "
                        + employee.getLastName();

        String qrUrl =
                "/verify-employee/"
                        + employee.getQrToken();

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "employeeName",
                fullName
        );

        model.addAttribute(
                "qrToken",
                employee.getQrToken()
        );

        model.addAttribute(
                "qrUrl",
                qrUrl
        );

        return "employee/qr";
    }

    // ============================================================
    // REGENERATE QR
    // ============================================================

    @PostMapping("/admin/employee-qr/{id}/regenerate")
    public String regenerateQr(
            @PathVariable Long id) {

        Optional<Employee> result =
                employeeRepository.findById(id);

        if (result.isEmpty()) {

            return "redirect:/admin/employees";
        }

        Employee employee =
                result.get();

        qrCodeService.regenerateToken(
                employee
        );

        return "redirect:/admin/employee-qr/"
                + id;
    }

    // ============================================================
    // FIND LOGGED-IN EMPLOYEE
    // ============================================================

    private Employee getLoggedInEmployee(
            Authentication authentication) {

        if (authentication == null) {

            return null;
        }

        String username =
                authentication.getName();

        return employeeRepository
                .findByUserUsername(username)
                .orElse(null);
    }
}

