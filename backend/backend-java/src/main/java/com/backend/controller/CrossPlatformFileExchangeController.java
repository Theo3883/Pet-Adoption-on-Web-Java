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

import java.io.IOException;
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
    
    /**
     * Export files as a downloadable package
     */
    @PostMapping("/api/files/export")
    public ResponseEntity<?> exportFilePackage(
            @Valid @RequestBody FilePackageRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }
            
            log.info("Exporting {} files for user {}", request.getFileIds().size(), userId);
            
            // Create file package
            SerializableFilePackage filePackage = exchangeService.exportFilePackage(request, userId);
            
            // Convert to JSON
            String jsonContent = objectMapper.writeValueAsString(filePackage);
            byte[] packageData = jsonContent.getBytes();
            
            // Compress if requested
            if (request.isCompressPackage()) {
                packageData = exchangeService.compressData(packageData);
            }
            
            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s.%s", 
                request.getPackageName() != null ? request.getPackageName().replaceAll("[^a-zA-Z0-9]", "_") : "FilePackage",
                timestamp,
                request.isCompressPackage() ? "pkg.gz" : "pkg.json");
            
            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(request.isCompressPackage() ? 
                MediaType.APPLICATION_OCTET_STREAM : MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(packageData.length);
            
            log.info("Successfully exported package {} with {} files", filePackage.getPackageId(), filePackage.getTotalFiles());
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(packageData);
                
        } catch (Exception e) {
            log.error("Error exporting file package: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to export files: " + e.getMessage()));
        }
    }
    
    /**
     * Get export package as JSON (for API integration)
     */
    @PostMapping("/api/files/export/json")
    public ResponseEntity<?> exportFilePackageAsJson(
            @Valid @RequestBody FilePackageRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }
            
            // Create file package
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
    
    /**
     * Import files from uploaded package
     */
    @PostMapping("/api/files/import")
    public ResponseEntity<?> importFilePackage(
            @RequestParam("packageFile") MultipartFile packageFile,
            @RequestParam(value = "isCompressed", defaultValue = "false") boolean isCompressed,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }
            
            if (packageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No package file uploaded"));
            }
            
            log.info("Importing file package for user {}", userId);
            
            // Read package data
            byte[] packageData = packageFile.getBytes();
            
            // Decompress if necessary
            if (isCompressed) {
                packageData = exchangeService.decompressData(packageData);
            }
            
            // Parse JSON to SerializableFilePackage
            String jsonContent = new String(packageData);
            SerializableFilePackage filePackage = objectMapper.readValue(jsonContent, SerializableFilePackage.class);
            
            // Import files
            Map<String, Object> importResult = exchangeService.importFilePackage(filePackage, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package imported successfully");
            response.put("importResult", importResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error importing file package: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to import package: " + e.getMessage()));
        }
    }
    
    /**
     * Import files from JSON package (for API integration)
     */
    @PostMapping("/api/files/import/json")
    public ResponseEntity<?> importFilePackageFromJson(
            @RequestBody SerializableFilePackage filePackage,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }
            
            log.info("Importing file package from JSON for user {}", userId);
            
            // Import files
            Map<String, Object> importResult = exchangeService.importFilePackage(filePackage, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Package imported successfully");
            response.put("importResult", importResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error importing file package from JSON: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to import package: " + e.getMessage()));
        }
    }
    
    /**
     * Get package information without importing
     */
    @PostMapping("/api/files/package/info")
    public ResponseEntity<?> getPackageInfo(
            @RequestParam("packageFile") MultipartFile packageFile,
            @RequestParam(value = "isCompressed", defaultValue = "false") boolean isCompressed,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }
            
            if (packageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No package file uploaded"));
            }
            
            // Read and parse package
            byte[] packageData = packageFile.getBytes();
            if (isCompressed) {
                packageData = exchangeService.decompressData(packageData);
            }
            
            String jsonContent = new String(packageData);
            SerializableFilePackage filePackage = objectMapper.readValue(jsonContent, SerializableFilePackage.class);
            
            // Create info response (without file content)
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
            
            // File summary (without content)
            if (filePackage.getFiles() != null) {
                packageInfo.put("fileSummary", filePackage.getFiles().stream()
                    .map(file -> Map.of(
                        "fileName", file.getFileName(),
                        "mediaType", file.getMediaType(),
                        "fileSize", file.getFileSize(),
                        "animalName", file.getAnimalName() != null ? file.getAnimalName() : "N/A"
                    ))
                    .toList());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("packageInfo", packageInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error reading package info: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("Failed to read package info: " + e.getMessage()));
        }
    }
    
    /**
     * Export package metadata only (for preview)
     */
    @PostMapping("/api/files/export/preview")
    public ResponseEntity<?> exportPackagePreview(
            @Valid @RequestBody FilePackageRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // Extract user ID from JWT token
            Long userId = extractUserIdFromToken(httpRequest);
            if (userId == null) {
                return ResponseEntity.status(401).body(createErrorResponse("Authentication required"));
            }
            
            // Create preview request (no file content)
            FilePackageRequest previewRequest = FilePackageRequest.builder()
                .fileIds(request.getFileIds())
                .packageName(request.getPackageName())
                .description(request.getDescription())
                .includeMetadata(true)
                .compressPackage(false)
                .build();
            
            // This would need to be modified to create a preview without file content
            // For now, we'll create the full package but indicate it's a preview
            SerializableFilePackage filePackage = exchangeService.exportFilePackage(previewRequest, userId);
            
            // Remove file content for preview
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
    
    /**
     * Extract user ID from JWT token
     */
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
    
    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", LocalDateTime.now());
        return error;
    }
} 