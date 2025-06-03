package com.backend.controller;

import com.backend.dto.FilePackageRequest;
import com.backend.model.SerializableFilePackage;
import com.backend.service.CrossPlatformFileExchangeService;
import com.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CrossPlatformFileExchangeController {

    private final CrossPlatformFileExchangeService exchangeService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @PostMapping("/api/files/export")
    public ResponseEntity<?> exportFilePackage(
            @Valid @RequestBody FilePackageRequest request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            log.info("Exporting {} files for user {}", request.getFileIds().size(), userId);
            SerializableFilePackage filePackage = exchangeService.exportFilePackage(request, userId);

            String jsonContent = objectMapper.writeValueAsString(filePackage);
            byte[] packageData = jsonContent.getBytes();

            if (request.isCompressPackage()) {
                packageData = exchangeService.compressData(packageData);
            }

            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            HttpHeaders headers = getHttpHeaders(request, timestamp, packageData);

            log.info("Successfully exported package {} with {} files", filePackage.getPackageId(),
                    filePackage.getTotalFiles());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(packageData);

        } catch (Exception e) {
            log.error("Error exporting file package: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to export files: " + e.getMessage()));
        }
    }

    private static HttpHeaders getHttpHeaders(FilePackageRequest request, String timestamp, byte[] packageData) {
        String filename = String.format("%s_%s.%s",
                request.getPackageName() != null ? request.getPackageName().replaceAll("[^a-zA-Z0-9]", "_")
                        : "FilePackage",
                timestamp,
                request.isCompressPackage() ? "pkg.gz" : "pkg.json");

        // Set response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                request.isCompressPackage() ? MediaType.APPLICATION_OCTET_STREAM : MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(packageData.length);
        return headers;
    }

    @PostMapping("/api/files/export/json")
    public ResponseEntity<?> exportFilePackageAsJson(
            @Valid @RequestBody FilePackageRequest request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            SerializableFilePackage filePackage = exchangeService.exportFilePackage(request, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("package", filePackage);
            response.put("exportedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error exporting file package as JSON: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to export files: " + e.getMessage()));
        }
    }

    @PostMapping("/api/files/import")
    public ResponseEntity<?> importFilePackage(
            @RequestParam("packageFile") MultipartFile packageFile,
            @RequestParam(value = "isCompressed", defaultValue = "false") boolean isCompressed,
            HttpServletRequest httpRequest) {

        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            if (packageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No package file uploaded"));
            }

            log.info("Importing file package for user {}", userId);
            byte[] packageData = packageFile.getBytes();

            if (isCompressed) {
                packageData = exchangeService.decompressData(packageData);
            }

            String jsonContent = new String(packageData);
            SerializableFilePackage filePackage = objectMapper.readValue(jsonContent, SerializableFilePackage.class);
            return getResponseEntity(userId, filePackage);

        } catch (Exception e) {
            log.error("Error importing file package: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to import package: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> getResponseEntity(Long userId, SerializableFilePackage filePackage) {
        Map<String, Object> importResult = exchangeService.importFilePackage(filePackage, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Package imported successfully");
        response.put("importResult", importResult);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/files/import/json")
    public ResponseEntity<?> importFilePackageFromJson(
            @RequestBody SerializableFilePackage filePackage,
            HttpServletRequest httpRequest) {

        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            log.info("Importing file package from JSON for user {}", userId);
            return getResponseEntity(userId, filePackage);

        } catch (Exception e) {
            log.error("Error importing file package from JSON: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to import package: " + e.getMessage()));
        }
    }

    @PostMapping("/api/files/package/info")
    public ResponseEntity<?> getPackageInfo(
            @RequestParam("packageFile") MultipartFile packageFile,
            @RequestParam(value = "isCompressed", defaultValue = "false") boolean isCompressed,
            HttpServletRequest httpRequest) {

        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            if (packageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No package file uploaded"));
            }

            byte[] packageData = packageFile.getBytes();
            if (isCompressed) {
                packageData = exchangeService.decompressData(packageData);
            }

            String jsonContent = new String(packageData);
            SerializableFilePackage filePackage = objectMapper.readValue(jsonContent, SerializableFilePackage.class);

            Map<String, Object> packageInfo = new HashMap<>();
            packageInfo.put("packageId", filePackage.getPackageId());
            packageInfo.put("packageName", filePackage.getPackageName());
            packageInfo.put("description", filePackage.getDescription());
            packageInfo.put("createdAt", filePackage.getCreatedAt());
            packageInfo.put("createdBy", filePackage.getCreatedBy());
            packageInfo.put("sourceSystem", filePackage.getSourceSystem());
            packageInfo.put("sourceVersion", filePackage.getSourceVersion());
            packageInfo.put("totalFiles", filePackage.getTotalFiles());
            packageInfo.put("totalSizeBytes", filePackage.getTotalSizeBytes());
            packageInfo.put("isCompressed", filePackage.isCompressed());
            packageInfo.put("includesMetadata", filePackage.isIncludesMetadata());
            packageInfo.put("supportedFormats", filePackage.getSupportedFormats());

            if (filePackage.getFiles() != null) {
                packageInfo.put("fileSummary", filePackage.getFiles().stream()
                        .map(file -> Map.of(
                                "fileName", file.getFileName(),
                                "mediaType", file.getMediaType(),
                                "fileSize", file.getFileSize(),
                                "animalName", file.getAnimalName() != null ? file.getAnimalName() : "N/A"))
                        .toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("packageInfo", packageInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error reading package info: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Failed to read package info: " + e.getMessage()));
        }
    }

    @PostMapping("/api/files/export/preview")
    public ResponseEntity<?> exportPackagePreview(
            @Valid @RequestBody FilePackageRequest request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }

            FilePackageRequest previewRequest = FilePackageRequest.builder()
                    .fileIds(request.getFileIds())
                    .packageName(request.getPackageName())
                    .description(request.getDescription())
                    .includeMetadata(true)
                    .compressPackage(false)
                    .build();

            SerializableFilePackage filePackage = exchangeService.exportFilePackage(previewRequest, userId);

            if (filePackage.getFiles() != null) {
                filePackage.getFiles().forEach(file -> file.setFileContent(null));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("preview", filePackage);
            response.put("isPreview", true);
            response.put("note", "File content excluded from preview");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating package preview: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to create preview: " + e.getMessage()));
        }
    }

    private Long extractUserIdFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtService.extractUserId(token);
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", LocalDateTime.now());
        return error;
    }
}