package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class QrCodeService {

    private final EmployeeRepository employeeRepository;

    private final SecureRandom secureRandom =
            new SecureRandom();

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "abcdefghijklmnopqrstuvwxyz"
                    + "0123456789";

    private static final int TOKEN_LENGTH = 48;

    public QrCodeService(
            EmployeeRepository employeeRepository) {

        this.employeeRepository =
                employeeRepository;
    }

    // ============================================================
    // GENERATE RANDOM QR TOKEN
    // ============================================================

    public String generateToken() {

        StringBuilder token =
                new StringBuilder(TOKEN_LENGTH);

        for (int i = 0;
             i < TOKEN_LENGTH;
             i++) {

            int index =
                    secureRandom.nextInt(
                            CHARACTERS.length()
                    );

            token.append(
                    CHARACTERS.charAt(index)
            );
        }

        return token.toString();
    }

    // ============================================================
    // GENERATE UNIQUE TOKEN
    // ============================================================

    public String generateUniqueToken() {

        String token;

        do {

            token = generateToken();

        } while (
                employeeRepository
                        .existsByQrToken(token)
        );

        return token;
    }

    // ============================================================
    // ENSURE EMPLOYEE HAS QR TOKEN
    // ============================================================

    @Transactional
    public String ensureQrToken(
            Employee employee) {

        if (employee.getQrToken() == null
                || employee.getQrToken().isBlank()) {

            employee.setQrToken(
                    generateUniqueToken()
            );

            employeeRepository.save(employee);
        }

        return employee.getQrToken();
    }

    // ============================================================
    // GENERATE TOKENS FOR EXISTING EMPLOYEES
    // ============================================================

    @Transactional
    public void generateMissingTokens() {

        List<Employee> employees =
                employeeRepository.findAll();

        for (Employee employee : employees) {

            if (employee.getQrToken() == null
                    || employee.getQrToken().isBlank()) {

                employee.setQrToken(
                        generateUniqueToken()
                );

                employeeRepository.save(employee);
            }
        }
    }

    // ============================================================
    // REGENERATE EMPLOYEE QR TOKEN
    // ============================================================

    @Transactional
    public String regenerateToken(
            Employee employee) {

        String newToken =
                generateUniqueToken();

        employee.setQrToken(newToken);

        employeeRepository.save(employee);

        return newToken;
    }

    // ============================================================
    // BUILD QR URL
    // ============================================================

    public String buildQrUrl(
            Employee employee,
            String baseUrl) {

        ensureQrToken(employee);

        return baseUrl
                + "/verify-employee/"
                + employee.getQrToken();
    }
}

