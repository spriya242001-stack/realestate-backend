package com.example.realestate_backend.service;

import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.Set;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) throws IOException {

        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024
                || file.getContentType() == null
                || !Set.of("image/png", "image/jpeg", "image/gif").contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid image upload");
        }
        byte[] bytes = file.getBytes();
        if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
            throw new IllegalArgumentException("Invalid image content");
        }
        Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, Map.of("resource_type", "image"));
        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null) {
            throw new IOException("Image provider returned no image URL");
        }
        return secureUrl.toString();
    }
}