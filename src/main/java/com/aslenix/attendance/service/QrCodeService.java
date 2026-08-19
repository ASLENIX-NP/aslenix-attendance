package com.aslenix.attendance.service;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

@Service
public class QrCodeService {

    private final EmployeeRepository employeeRepository;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public QrCodeService(
            EmployeeRepository employeeRepository) {

        this.employeeRepository =
                employeeRepository;
    }

    // ============================================================
    // GENERATE QR TOKENS FOR EXISTING EMPLOYEES
    // ============================================================

    @PostConstruct
    public void initializeEmployeeQrTokens() {

        List<Employee> employees =
                employeeRepository.findAll();

        boolean changed = false;

        for (Employee employee : employees) {

            if (employee.getQrToken() == null
                    || employee.getQrToken().isBlank()) {

                employee.setQrToken(
                        generateUniqueToken()
                );

                employeeRepository.save(employee);

                changed = true;
            }
        }

        if (changed) {

            System.out.println(
                    "ASLENIX: Employee QR tokens initialized."
            );
        }
    }

    // ============================================================
    // GENERATE UNIQUE TOKEN
    // ============================================================

    public String generateUniqueToken() {

        String token;

        do {

            token = generateRandomToken();

        } while (
                employeeRepository
                        .findByQrToken(token)
                        .isPresent()
        );

        return token;
    }

    // ============================================================
    // RANDOM TOKEN
    // ============================================================

    private String generateRandomToken() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(bytes);

        StringBuilder token =
                new StringBuilder();

        for (byte b : bytes) {

            token.append(
                    String.format(
                            "%02x",
                            b
                    )
            );
        }

        return token.toString();
    }

    // ============================================================
    // GENERATE QR IMAGE
    // ============================================================

    public byte[] generateQrImage(
            String qrToken,
            int width,
            int height) {

        try {

            QRCodeWriter qrCodeWriter =
                    new QRCodeWriter();

            BitMatrix bitMatrix =
                    qrCodeWriter.encode(
                            qrToken,
                            BarcodeFormat.QR_CODE,
                            width,
                            height
                    );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (
                WriterException |
                IOException exception) {

            throw new RuntimeException(
                    "Unable to generate QR code.",
                    exception
            );
        }
    }

    // ============================================================
    // ENSURE EMPLOYEE HAS QR
    // ============================================================

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
}