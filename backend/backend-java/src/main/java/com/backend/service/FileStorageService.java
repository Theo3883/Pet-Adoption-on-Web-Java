package com.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    public String storeFile(MultipartFile file, String mediaType) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        Path uploadPath = Paths.get(uploadDir, mediaType);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFilename = UUID.randomUUID() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File stored successfully: {}", filePath);

        return uniqueFilename;
    }

    public byte[] loadFile(String mediaType, String filename) throws IOException {
        Path filePath = Paths.get(uploadDir, mediaType, filename);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filename);
        }

        return Files.readAllBytes(filePath);
    }

    public boolean deleteFile(String mediaType, String filename) {
        try {
            Path filePath = Paths.get(uploadDir, mediaType, filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", filePath);
                return true;
            } else {
                log.warn("File not found, cannot delete: {}", filePath);
                return false;
            }
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }

    public String getContentType(String filename) {
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/avi";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }

    public boolean fileExists(String mediaType, String filename) {
        Path filePath = Paths.get(uploadDir, mediaType, filename);
        return Files.exists(filePath);
    }

    public String getPublicUrl(String mediaType, String filename) {
        return "/server/" + mediaType + "/" + filename;
    }
}