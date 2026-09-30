package com.aslenix.attendance.config;

import com.aslenix.attendance.entity.Employee;
import com.aslenix.attendance.repository.EmployeeRepository;
import com.aslenix.attendance.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class ImageKitMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImageKitMigrationRunner.class);

    private final ImageKitConfig imageKitConfig;
    private final FileUploadService fileUploadService;
    private final EmployeeRepository employeeRepository;

    public ImageKitMigrationRunner(ImageKitConfig imageKitConfig,
                                  FileUploadService fileUploadService,
                                  EmployeeRepository employeeRepository) {
        this.imageKitConfig = imageKitConfig;
        this.fileUploadService = fileUploadService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!fileUploadService.isImageKitActive()) {
            log.info("ImageKit is not active. Existing photos remain on local disk.");
            return;
        }

        log.info("ImageKit is active! Checking for locally saved photos to migrate to ImageKit...");
        try {
            List<Employee> employees = employeeRepository.findAll();
            int migratedCount = 0;

            for (Employee employee : employees) {
                String currentPhotoUrl = employee.getPhotoUrl();
                if (currentPhotoUrl == null || !currentPhotoUrl.startsWith("/uploads/photos/")) {
                    continue;
                }

                String fileName = currentPhotoUrl.replace("/uploads/photos/", "");
                Path localFile = fileUploadService.getUploadRoot().resolve(fileName).normalize();

                if (Files.exists(localFile) && Files.isRegularFile(localFile)) {
                    try {
                        log.info("Migrating local photo to ImageKit for employee ID {} ({}) -> {}",
                                employee.getId(), employee.getEmail(), fileName);
                        String cloudUrl = fileUploadService.uploadLocalFileToImageKit(localFile, fileName);
                        if (cloudUrl != null && !cloudUrl.isBlank()) {
                            employee.setPhotoUrl(cloudUrl);
                            employeeRepository.save(employee);
                            migratedCount++;
                            log.info("Successfully migrated employee {} photo to ImageKit: {}",
                                    employee.getId(), cloudUrl);
                        }
                    } catch (Exception e) {
                        log.error("Failed to migrate photo '{}' for employee {}: {}",
                                fileName, employee.getId(), e.getMessage());
                    }
                } else {
                    log.warn("Local photo file not found on disk for employee {}: {}",
                            employee.getId(), localFile);
                }
            }

            if (migratedCount > 0) {
                log.info("ImageKit Migration Complete: Successfully migrated {} photo(s) to ImageKit!", migratedCount);
            } else {
                log.info("ImageKit Migration Check: No pending local photos needed migration.");
            }
        } catch (Exception e) {
            log.error("Error during ImageKit photo migration: {}", e.getMessage(), e);
        }
    }
}
