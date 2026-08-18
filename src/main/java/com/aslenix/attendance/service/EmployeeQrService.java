package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeQrService {

    private final EmployeeRepository employeeRepository;

    public EmployeeQrService(
            EmployeeRepository employeeRepository) {

        this.employeeRepository =
                employeeRepository;
    }

    // ============================================================
    // GENERATE QR TOKEN
    // ============================================================

    public String generateToken() {

        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    // ============================================================
    // ENSURE EMPLOYEE HAS QR TOKEN
    // ============================================================

    public String ensureQrToken(Employee employee) {

        if (employee.getQrToken() == null
                || employee.getQrToken().isBlank()) {

            employee.setQrToken(
                    generateToken()
            );

            employeeRepository.save(employee);
        }

        return employee.getQrToken();
    }

    // ============================================================
    // GENERATE TOKENS FOR EXISTING EMPLOYEES
    // ============================================================

    public void generateMissingTokens() {

        List<Employee> employees =
                employeeRepository.findAll();

        for (Employee employee : employees) {

            if (employee.getQrToken() == null
                    || employee.getQrToken().isBlank()) {

                employee.setQrToken(
                        generateToken()
                );

                employeeRepository.save(employee);
            }
        }
    }
}