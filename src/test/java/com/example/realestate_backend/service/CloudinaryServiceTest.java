package com.example.realestate_backend.service;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CloudinaryServiceTest {
    @Test void rejectsNonImagesBeforeCallingProvider() {
        Cloudinary provider = mock(Cloudinary.class);
        CloudinaryService service = new CloudinaryService(provider);
        assertThrows(IllegalArgumentException.class, () -> service.uploadImage(
                new MockMultipartFile("imageFile", "payload.html", "text/html", "<script/>".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> service.uploadImage(
                new MockMultipartFile("imageFile", "fake.png", "image/png", "not an image".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> service.uploadImage(
                new MockMultipartFile("imageFile", "unknown", null, new byte[]{1})));
        verifyNoInteractions(provider);
    }
}
