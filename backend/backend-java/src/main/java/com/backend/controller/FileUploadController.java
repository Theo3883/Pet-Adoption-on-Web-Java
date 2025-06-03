package com.backend.controller;

import com.backend.exception.FileException;
import com.backend.exception.ValidationException;
import com.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", defaultValue = "photo") String mediaType) {

        if (file.isEmpty()) {
            throw ValidationException.missingRequiredField("file");
        }

        if ("photo".equals(mediaType)) {
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("video/")) {
                    mediaType = "video";
                } else if (contentType.startsWith("audio/")) {
                    mediaType = "audio";
                }
            }
        }

        try {
            String filename = fileStorageService.storeFile(file, mediaType);
            String publicUrl = fileStorageService.getPublicUrl(mediaType, filename);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", publicUrl);
            response.put("filename", filename);
            response.put("mediaType", mediaType);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw FileException.fileUploadFailed(file.getOriginalFilename());
        }
    }

    @GetMapping("/server/{mediaType}/{filename}")
    public ResponseEntity<?> getMedia(
            @PathVariable String mediaType,
            @PathVariable String filename) {

        try {
            if (!fileStorageService.fileExists(mediaType, filename)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = fileStorageService.loadFile(mediaType, filename);
            String contentType = fileStorageService.getContentType(filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000") // 1 year cache
                    .body(fileContent);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error loading file: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/server/{mediaType}/{filename}")
    public ResponseEntity<?> deleteMedia(
            @PathVariable String mediaType,
            @PathVariable String filename) {

        try {
            boolean deleted = fileStorageService.deleteFile(mediaType, filename);

            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "File deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "File not found");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error deleting file: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}