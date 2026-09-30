package com.aslenix.attendance.service;

import com.aslenix.attendance.config.ImageKitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FileUploadServiceTest {

    @Test
    void testLocalFallbackWhenImageKitNotConfigured() throws IOException {
        ImageKitConfig config = new ImageKitConfig();
        FileUploadService service = new FileUploadService(config, null);

        assertFalse(service.isImageKitActive());

        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "test_avatar.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        String photoUrl = service.storeEmployeePhoto(file);
        assertNotNull(photoUrl);
        assertTrue(photoUrl.startsWith("/uploads/photos/"));
        assertTrue(photoUrl.endsWith(".jpg"));

        // Clean up
        service.deleteEmployeePhoto(photoUrl);
    }

    @Test
    void testInvalidExtensionThrowsException() {
        ImageKitConfig config = new ImageKitConfig();
        FileUploadService service = new FileUploadService(config, null);

        MockMultipartFile badFile = new MockMultipartFile(
                "photo",
                "malicious.exe",
                "application/octet-stream",
                new byte[]{1, 2}
        );

        assertThrows(IllegalArgumentException.class, () -> service.storeEmployeePhoto(badFile));
    }
}
