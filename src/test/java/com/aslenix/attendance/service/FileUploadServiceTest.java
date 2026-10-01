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

    @Test
    void testImageKitFailureThrowsExceptionAndDoesNotSaveLocally() {
        ImageKitConfig config = org.mockito.Mockito.mock(ImageKitConfig.class);
        org.mockito.Mockito.when(config.isConfigured()).thenReturn(true);
        org.mockito.Mockito.when(config.getFolder()).thenReturn("/employee-photos");
        org.mockito.Mockito.when(config.getPublicKey()).thenReturn("public_test");

        io.imagekit.client.ImageKitClient client = org.mockito.Mockito.mock(io.imagekit.client.ImageKitClient.class);
        io.imagekit.services.blocking.FileService fileService = org.mockito.Mockito.mock(io.imagekit.services.blocking.FileService.class);
        org.mockito.Mockito.when(client.files()).thenReturn(fileService);
        org.mockito.Mockito.when(fileService.upload(org.mockito.Mockito.any())).thenThrow(new RuntimeException("Your request contains expired private API key."));

        FileUploadService service = new FileUploadService(config, client);
        assertTrue(service.isImageKitActive());

        MockMultipartFile file = new MockMultipartFile(
                "photo",
                "avatar.jpg",
                "image/jpeg",
                new byte[]{10, 20, 30}
        );

        IOException thrown = assertThrows(IOException.class, () -> service.storeEmployeePhoto(file));
        assertTrue(thrown.getMessage().contains("Failed to upload photo to ImageKit"));
    }
}
