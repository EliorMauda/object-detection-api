package com.objectdetection.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile multipartFile) throws IOException {
        File file = convertMultipartToFile(multipartFile);
        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
            }
            return (String) uploadResult.get("url");
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw e;
        }
    }

    public String uploadImage(String imageUrl) throws IOException {
        try {
            Map uploadResult = cloudinary.uploader().upload(imageUrl, ObjectUtils.emptyMap());
            return (String) uploadResult.get("url");
        } catch (IOException e) {
            log.error("Failed to upload image from URL to Cloudinary: {}", imageUrl, e);
            throw e;
        }
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }
}