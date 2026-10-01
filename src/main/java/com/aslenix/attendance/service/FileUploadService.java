package com.aslenix.attendance.service;

import com.aslenix.attendance.config.ImageKitConfig;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.assets.AssetListParams;
import io.imagekit.models.assets.AssetListResponse;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);
    private final Path uploadRoot = Paths.get("uploads", "photos").toAbsolutePath().normalize();

    private final ImageKitConfig imageKitConfig;
    private final ImageKitClient imageKitClient;

    public FileUploadService(ImageKitConfig imageKitConfig, @Autowired(required = false) ImageKitClient imageKitClient) {
        this.imageKitConfig = imageKitConfig;
        this.imageKitClient = imageKitClient;
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            log.error("Could not initialize upload directory: {}", uploadRoot, e);
        }
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public boolean isImageKitActive() {
        return imageKitConfig != null && imageKitConfig.isConfigured() && imageKitClient != null;
    }

    /**
     * Stores an uploaded employee photo and returns its web-accessible URL.
     * When ImageKit is configured via environment variables, the photo is stored in ImageKit.
     * Otherwise, falls back to local disk storage.
     *
     * @param file the multipart file uploaded by the client
     * @return the photo URL (ImageKit CDN URL or local /uploads/photos/... path) or null if empty
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

        String uniqueFileName = "emp_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        byte[] fileBytes = file.getBytes();

        if (isImageKitActive()) {
            return uploadToImageKit(fileBytes, uniqueFileName);
        }

        // Default / fallback local disk storage ONLY when ImageKit is not configured/active
        return storeLocally(fileBytes, uniqueFileName);
    }

    /**
     * Uploads raw image bytes to ImageKit and returns its public CDN URL.
     */
    public String uploadToImageKit(byte[] fileBytes, String fileName) throws IOException {
        if (!isImageKitActive()) {
            throw new IllegalStateException("ImageKit is not active.");
        }

        String folder = imageKitConfig.getFolder();
        FileUploadParams.Builder builder = FileUploadParams.builder()
                .file(new ByteArrayInputStream(fileBytes))
                .fileName(fileName)
                .folder(folder)
                .useUniqueFileName(true);

        String pubKey = imageKitConfig.getPublicKey();
        if (pubKey != null && !pubKey.isBlank()) {
            builder.publicKey(pubKey);
        }

        try {
            FileUploadResponse response = imageKitClient.files().upload(builder.build());
            String photoUrl = response.url().orElse(null);

            if (photoUrl == null || photoUrl.isBlank()) {
                String endpoint = imageKitConfig.getUrlEndpoint();
                if (endpoint != null && !endpoint.isBlank()) {
                    if (endpoint.endsWith("/")) {
                        endpoint = endpoint.substring(0, endpoint.length() - 1);
                    }
                    String filePath = response.filePath().orElse(folder + "/" + fileName);
                    if (!filePath.startsWith("/")) {
                        filePath = "/" + filePath;
                    }
                    photoUrl = endpoint + filePath;
                }
            }

            log.info("Uploaded employee photo to ImageKit: fileName={}, fileId={}, url={}",
                    fileName, response.fileId().orElse("N/A"), photoUrl);
            return photoUrl;
        } catch (Exception e) {
            log.error("ImageKit upload error for '{}': {}", fileName, e.getMessage(), e);
            throw new IOException("Failed to upload photo to ImageKit: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads an existing local file to ImageKit.
     */
    public String uploadLocalFileToImageKit(Path localFilePath, String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(localFilePath);
        return uploadToImageKit(bytes, fileName);
    }

    private String storeLocally(byte[] fileBytes, String uniqueFileName) throws IOException {
        if (!Files.exists(uploadRoot)) {
            Files.createDirectories(uploadRoot);
        }
        Path targetLocation = uploadRoot.resolve(uniqueFileName);
        Files.copy(new ByteArrayInputStream(fileBytes), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored employee photo locally: {} at {}", uniqueFileName, targetLocation);
        return "/uploads/photos/" + uniqueFileName;
    }

    /**
     * Deletes an employee photo from ImageKit or disk based on URL format.
     *
     * @param photoUrl the web URL of the photo (ImageKit URL or local /uploads/photos/ path)
     */
    public void deleteEmployeePhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }

        if (photoUrl.startsWith("/uploads/photos/")) {
            deleteLocalPhoto(photoUrl);
            return;
        }

        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            if (isImageKitActive()) {
                deleteFromImageKit(photoUrl);
            }
        }
    }

    private void deleteLocalPhoto(String photoUrl) {
        try {
            String fileName = photoUrl.replace("/uploads/photos/", "");
            Path filePath = uploadRoot.resolve(fileName).normalize();
            if (filePath.startsWith(uploadRoot) && Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted local employee photo: {}", fileName);
            }
        } catch (Exception e) {
            log.warn("Could not delete local employee photo at {}: {}", photoUrl, e.getMessage());
        }
    }

    private void deleteFromImageKit(String photoUrl) {
        try {
            String cleanUrl = photoUrl;
            if (cleanUrl.contains("?")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
            }
            String fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
            if (fileName.isBlank()) {
                return;
            }

            AssetListParams listParams = AssetListParams.builder()
                    .searchQuery("name = \"" + fileName + "\"")
                    .limit(5)
                    .build();

            List<AssetListResponse> assets = imageKitClient.assets().list(listParams);
            if (assets != null && !assets.isEmpty()) {
                for (AssetListResponse asset : assets) {
                    String fileId = asset.file().flatMap(io.imagekit.models.files.File::fileId).orElse(null);
                    if (fileId != null && !fileId.isBlank()) {
                        imageKitClient.files().delete(fileId);
                        log.info("Deleted employee photo from ImageKit: fileName={}, fileId={}", fileName, fileId);
                    }
                }
            } else {
                log.debug("No matching file found on ImageKit for delete query: {}", fileName);
            }
        } catch (Exception e) {
            log.warn("Could not delete employee photo from ImageKit at {}: {}", photoUrl, e.getMessage());
        }
    }
}
