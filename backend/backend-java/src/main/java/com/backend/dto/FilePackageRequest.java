package com.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilePackageRequest {
    
    @NotEmpty(message = "File IDs cannot be empty")
    @Size(max = 100, message = "Cannot export more than 100 files at once")
    private List<Long> fileIds;
    
    @Size(max = 255, message = "Package name must not exceed 255 characters")
    private String packageName;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @Builder.Default
    private boolean includeMetadata = true;
    @Builder.Default
    private boolean compressPackage = false;
} 