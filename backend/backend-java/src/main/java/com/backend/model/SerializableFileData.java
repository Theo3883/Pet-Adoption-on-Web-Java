package com.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerializableFileData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Original file metadata
    private Long originalId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDate uploadDate;
    
    // File content (Base64 encoded for JSON compatibility)
    private byte[] fileContent;
    
    // Animal metadata (if associated)
    private Long animalId;
    private String animalName;
    private String animalSpecies;
    private String animalBreed;
    
    // Multimedia metadata
    private String mediaType; // photo, video, audio
    private String description;
    private String url; // original URL reference
    
    // Owner metadata
    private Long ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerEmail;
    
    // Export metadata
    private LocalDateTime exportedAt;
    private String exportedBy;
    @Builder.Default
    private String packageVersion = "1.0";
    
    // File integrity
    private String checksum;
    private String compressionType;
} 