package com.aslenix.attendance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private final Path uploadRoot = Paths.get("uploads", "photos").toAbsolutePath().normalize();

    public FileUploadService() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            log.error("Could not initialize upload directory: {}", uploadRoot, e);
        }
    }

    /**
     * Stores an uploaded employee photo and returns its web-accessible URL.
     *
     * @param file the multipart file uploaded by the client
     * @return the relative web path (e.g. /uploads/photos/emp_12345.jpg) or null if empty
     * @throws IOException if saving fails
     */
    public String storeEmployeePhoto(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // Validate image extension
        if (!extension.matches("\\.(jpg|jpeg|png|webp|gif)$")) {
            throw new IllegalArgumentException("Only JPG, JPEG, PNG, WEBP, and GIF images are allowed.");
        }

        // Ensure directory exists
        if (!Files.exists(uploadRoot)) {
            Files.createDirectories(uploadRoot);
        }

        String uniqueFileName = "emp_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path targetLocation = uploadRoot.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored employee photo: {} at {}", uniqueFileName, targetLocation);

        return "/uploads/photos/" + uniqueFileName;
    }

    /**
     * Deletes an employee photo from disk if it was saved locally.
     *
     * @param photoUrl the web URL of the photo (e.g. /uploads/photos/emp_123.jpg)
     */
    public void deleteEmployeePhoto(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith("/uploads/photos/")) {
            return;
        }

        try {
            String fileName = photoUrl.replace("/uploads/photos/", "");
            Path filePath = uploadRoot.resolve(fileName).normalize();
            if (filePath.startsWith(uploadRoot) && Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted old employee photo: {}", fileName);
            }
        } catch (Exception e) {
            log.warn("Could not delete employee photo at {}: {}", photoUrl, e.getMessage());
        }
    }
}
