package com.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerializableFilePackage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Package identification
    private String packageId;
    private String packageName;
    private String description;
    private String packageVersion = "1.0";
    
    // Package metadata
    private LocalDateTime createdAt;
    private String createdBy;
    private Long createdByUserId;
    private String sourceSystem;
    private String sourceVersion;
    
    // Package contents
    private List<SerializableFileData> files;
    private Integer totalFiles;
    private Long totalSizeBytes;
    
    // Package settings
    private boolean isCompressed;
    private boolean includesMetadata;
    private String compressionAlgorithm;
    
    // Integrity and security
    private String packageChecksum;
    private Map<String, Object> additionalMetadata;
    
    // Import/Export tracking
    private LocalDateTime lastModified;
    private List<String> exportHistory;
    private List<String> importHistory;
    
    // Compatibility
    private String minRequiredVersion;
    private List<String> supportedFormats;
} 