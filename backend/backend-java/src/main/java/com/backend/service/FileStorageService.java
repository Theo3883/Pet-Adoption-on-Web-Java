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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;
    
    private final Map<String, byte[]> fileCache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> fileLocks = new ConcurrentHashMap<>();

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
        String cacheKey = mediaType + "_" + filename;
        
        byte[] fileContent = fileCache.get(cacheKey);
        if (fileContent != null) {
            log.info("File loaded from cache: {}", filename);
            return fileContent;
        }

        ReentrantLock lock = fileLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            fileContent = fileCache.get(cacheKey);
            if (fileContent != null) {
                log.info("File loaded from cache (after lock): {}", filename);
                return fileContent;
            }
            
            Path relativeFilePath = Paths.get(uploadDir, mediaType, filename);
            if (Files.exists(relativeFilePath)) {
                fileContent = Files.readAllBytes(relativeFilePath);
                fileCache.put(cacheKey, fileContent);
                log.info("File loaded from relative path: {}", relativeFilePath);
                return fileContent;
            }
            
            log.warn("File not found at expected paths. Relative path: {}", relativeFilePath);
            
            try {
                Path mediaTypePath = Paths.get(uploadDir, mediaType);
                if (Files.exists(mediaTypePath)) {
                    try (var files = Files.list(mediaTypePath)) {
                        Optional<Path> matchedFile = files
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(filename))
                            .findFirst();
                        
                        if (matchedFile.isPresent()) {
                            fileContent = Files.readAllBytes(matchedFile.get());
                            fileCache.put(cacheKey, fileContent);
                            log.info("Found alternative file in relative path: {}", matchedFile.get());
                            return fileContent;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error searching for alternative file: {}", e.getMessage());
            }
            
            throw new IOException("File not found: " + filename);
        } finally {
            lock.unlock();
        }
    }

    public boolean deleteFile(String mediaType, String filename) {
        try {
            Path relativeFilePath = Paths.get(uploadDir, mediaType, filename);
            boolean deleted = false;
            
            if (Files.exists(relativeFilePath)) {
                Files.delete(relativeFilePath);
                log.info("File deleted successfully from relative path: {}", relativeFilePath);
                deleted = true;
            }
            
            if (deleted) {
                String cacheKey = mediaType + "_" + filename;
                fileCache.remove(cacheKey);
                fileLocks.remove(cacheKey);
                return true;
            } else {
                log.warn("File not found in any path, cannot delete: {}", filename);
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
        Path relativeFilePath = Paths.get(uploadDir, mediaType, filename);
        return Files.exists(relativeFilePath);
    }

    public String getPublicUrl(String mediaType, String filename) {
        return "/server/" + mediaType + "/" + filename;
    }
}