package com.aslenix.attendance.controller;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.QrCodeService;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee")
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
    // MY PROFILE
    // ============================================================

    @GetMapping("/profile")
    public String profile(
            Authentication authentication,
            Model model) {

        if (authentication == null) {

            return "redirect:/login";
        }

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {

            return "redirect:/login";
        }

        // Make sure employee has a QR token.
        qrCodeService.ensureQrToken(employee);

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "employeeName",
                employee.getFirstName()
                        + " "
                        + employee.getLastName()
        );

        model.addAttribute(
                "today",
                LocalDate.now()
        );

        return "employee/profile";
    }

    // ============================================================
    // MY QR IMAGE
    // ============================================================

    @GetMapping("/profile/qr")
    public ResponseEntity<byte[]> qrImage(
            Authentication authentication) {

        if (authentication == null) {

            return ResponseEntity
                    .status(401)
                    .build();
        }

        Employee employee =
                getLoggedInEmployee(authentication);

        if (employee == null) {

            return ResponseEntity
                    .status(404)
                    .build();
        }

        String qrToken =
                qrCodeService.ensureQrToken(
                        employee
                );

        byte[] qrImage =
                qrCodeService.generateQrImage(
                        qrToken,
                        500,
                        500
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + employee.getEmployeeCode()
                                + "-QR.png\""
                )
                .contentType(
                        MediaType.IMAGE_PNG
                )
                .body(qrImage);
    }

    // ============================================================
    // LOGGED-IN EMPLOYEE
    // ============================================================

    private Employee getLoggedInEmployee(
            Authentication authentication) {

        return employeeRepository
                .findByUserUsername(
                        authentication.getName()
                )
                .orElse(null);
    }
}