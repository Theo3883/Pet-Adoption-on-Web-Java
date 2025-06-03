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

    private Long originalId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDate uploadDate;

    private byte[] fileContent;

    private Long animalId;
    private String animalName;
    private String animalSpecies;
    private String animalBreed;

    private String mediaType; // photo, video, audio
    private String description;
    private String url;

    private Long ownerId;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerEmail;

    private LocalDateTime exportedAt;
    private String exportedBy;
    @Builder.Default
    private String packageVersion = "1.0";

    private String checksum;
    private String compressionType;
}