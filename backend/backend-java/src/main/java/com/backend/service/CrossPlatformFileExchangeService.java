package com.backend.service;

import com.backend.dto.FilePackageRequest;
import com.backend.model.*;
import com.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrossPlatformFileExchangeService {
    
    private final MultiMediaRepository multiMediaRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    
    @Value("${file.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.version:1.0}")
    private String appVersion;
    
    @Value("${app.name:Pet-Adoption-Backend}")
    private String appName;
    
    @Transactional(readOnly = true)
    public SerializableFilePackage exportFilePackage(FilePackageRequest request, Long userId) {
        try {
            log.info("Starting export of {} files for user {}", request.getFileIds().size(), userId);
            
            User exportUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            List<SerializableFileData> serializedFiles = request.getFileIds().stream()
                .map(fileId -> loadAndSerializeFile(fileId, request.isIncludeMetadata()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            long totalSize = serializedFiles.stream()
                .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0)
                .sum();
            
            SerializableFilePackage filePackage = SerializableFilePackage.builder()
                .packageId(UUID.randomUUID().toString())
                .packageName(request.getPackageName() != null ? request.getPackageName() : "Exported Files")
                .description(request.getDescription())
                .packageVersion("1.0")
                .createdAt(LocalDateTime.now())
                .createdBy(exportUser.getFirstName() + " " + exportUser.getLastName())
                .createdByUserId(userId)
                .sourceSystem(appName)
                .sourceVersion(appVersion)
                .files(serializedFiles)
                .totalFiles(serializedFiles.size())
                .totalSizeBytes(totalSize)
                .isCompressed(request.isCompressPackage())
                .includesMetadata(request.isIncludeMetadata())
                .compressionAlgorithm(request.isCompressPackage() ? "GZIP" : "NONE")
                .packageChecksum(calculatePackageChecksum(serializedFiles))
                .lastModified(LocalDateTime.now())
                .exportHistory(Arrays.asList("Exported on " + LocalDateTime.now()))
                .minRequiredVersion("1.0")
                .supportedFormats(Arrays.asList("JPEG", "PNG", "MP4", "MP3", "WAV"))
                .build();
            
            log.info("Successfully exported package {} with {} files", filePackage.getPackageId(), serializedFiles.size());
            return filePackage;
            
        } catch (Exception e) {
            log.error("Error exporting file package: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export file package: " + e.getMessage());
        }
    }
    
    @Transactional
    public Map<String, Object> importFilePackage(SerializableFilePackage filePackage, Long userId) {
        try {
            log.info("Starting import of package {} with {} files for user {}", 
                filePackage.getPackageId(), filePackage.getTotalFiles(), userId);
            
            validatePackageIntegrity(filePackage);
            
            User importUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            List<String> importedFiles = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();
            long totalBytesImported = 0;
            
            for (SerializableFileData fileData : filePackage.getFiles()) {
                try {
                    String result = importSerializedFile(fileData, importUser);
                    importedFiles.add(result);
                    totalBytesImported += fileData.getFileSize() != null ? fileData.getFileSize() : 0;
                    
                } catch (Exception e) {
                    log.error("Failed to import file {}: {}", fileData.getFileName(), e.getMessage());
                    failedFiles.add(fileData.getFileName() + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> importResult = new HashMap<>();
            importResult.put("packageId", filePackage.getPackageId());
            importResult.put("packageName", filePackage.getPackageName());
            importResult.put("totalFilesInPackage", filePackage.getTotalFiles());
            importResult.put("successfulImports", importedFiles.size());
            importResult.put("failedImports", failedFiles.size());
            importResult.put("importedFiles", importedFiles);
            importResult.put("failedFiles", failedFiles);
            importResult.put("totalBytesImported", totalBytesImported);
            importResult.put("importedAt", LocalDateTime.now());
            importResult.put("importedBy", importUser.getFirstName() + " " + importUser.getLastName());
            
            log.info("Import completed. Success: {}, Failed: {}", importedFiles.size(), failedFiles.size());
            return importResult;
            
        } catch (Exception e) {
            log.error("Error importing file package: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to import file package: " + e.getMessage());
        }
    }
    
    private SerializableFileData loadAndSerializeFile(Long fileId, boolean includeMetadata) {
        try {
            MultiMedia multiMedia = multiMediaRepository.findById(fileId)
                .orElse(null);
            
            if (multiMedia == null) {
                log.warn("File not found: {}", fileId);
                return null;
            }
            
            String fileName = extractFilenameFromUrl(multiMedia.getUrl());
            String mediaType = multiMedia.getMedia().toString();
            
            byte[] fileContent = fileStorageService.loadFile(mediaType, fileName);
            
            SerializableFileData.SerializableFileDataBuilder builder = SerializableFileData.builder()
                .originalId(multiMedia.getId())
                .fileName(fileName)
                .contentType(getContentTypeFromMediaType(multiMedia.getMedia()))
                .fileSize((long) fileContent.length)
                .uploadDate(multiMedia.getUploadDate())
                .fileContent(fileContent)
                .mediaType(mediaType)
                .description(multiMedia.getDescription())
                .url(multiMedia.getUrl())
                .exportedAt(LocalDateTime.now())
                .packageVersion("1.0")
                .checksum(calculateFileChecksum(fileContent));
            
            if (includeMetadata && multiMedia.getAnimal() != null) {
                Animal animal = multiMedia.getAnimal();
                builder.animalId(animal.getAnimalId())
                    .animalName(animal.getName())
                    .animalSpecies(animal.getSpecies())
                    .animalBreed(animal.getBreed());
                
                if (animal.getUser() != null) {
                    User owner = animal.getUser();
                    builder.ownerId(owner.getUserId())
                        .ownerFirstName(owner.getFirstName())
                        .ownerLastName(owner.getLastName())
                        .ownerEmail(owner.getEmail());
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error loading and serializing file {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    private String importSerializedFile(SerializableFileData fileData, User importUser) throws Exception {
        String calculatedChecksum = calculateFileChecksum(fileData.getFileContent());
        if (!calculatedChecksum.equals(fileData.getChecksum())) {
            throw new RuntimeException("File integrity check failed for: " + fileData.getFileName());
        }
        
        String newFileName = UUID.randomUUID().toString() + getFileExtension(fileData.getFileName());
        
        Path uploadPath = Paths.get(uploadDir, fileData.getMediaType());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(newFileName);
        Files.write(filePath, fileData.getFileContent());
        
        Animal animal = null;
        if (fileData.getAnimalId() != null && fileData.getAnimalName() != null) {
            animal = findOrCreateAnimalForImport(fileData, importUser);
        }
        
        MultiMedia multiMedia = new MultiMedia();
        multiMedia.setMedia(MultiMedia.MediaType.valueOf(fileData.getMediaType()));
        multiMedia.setUrl(fileStorageService.getPublicUrl(fileData.getMediaType(), newFileName));
        multiMedia.setDescription(fileData.getDescription() + " (Imported from package)");
        multiMedia.setUploadDate(fileData.getUploadDate());
        multiMedia.setAnimal(animal);
        
        multiMediaRepository.save(multiMedia);
        return newFileName;
    }
    
    private Animal findOrCreateAnimalForImport(SerializableFileData fileData, User importUser) {
        List<Animal> existingAnimals = animalRepository.findByUserUserId(importUser.getUserId());
        
        Optional<Animal> matchingAnimal = existingAnimals.stream()
            .filter(animal -> animal.getName().equals(fileData.getAnimalName()) &&
                            animal.getSpecies().equals(fileData.getAnimalSpecies()))
            .findFirst();
        
        if (matchingAnimal.isPresent()) {
            return matchingAnimal.get();
        }
        
        //placeholder
        Animal newAnimal = new Animal();
        newAnimal.setName(fileData.getAnimalName() + " (Imported)");
        newAnimal.setSpecies(fileData.getAnimalSpecies());
        newAnimal.setBreed(fileData.getAnimalBreed());
        newAnimal.setAge(0);
        newAnimal.setGender(Animal.Gender.male);
        newAnimal.setUser(importUser);
        
        return animalRepository.save(newAnimal);
    }
    
    private void validatePackageIntegrity(SerializableFilePackage filePackage) {
        if (filePackage.getFiles() == null || filePackage.getFiles().isEmpty()) {
            throw new RuntimeException("Package contains no files");
        }
        
        if (!filePackage.getTotalFiles().equals(filePackage.getFiles().size())) {
            throw new RuntimeException("Package file count mismatch");
        }
        
        String calculatedChecksum = calculatePackageChecksum(filePackage.getFiles());
        if (!calculatedChecksum.equals(filePackage.getPackageChecksum())) {
            throw new RuntimeException("Package integrity check failed");
        }
    }
    
    private String calculateFileChecksum(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating checksum", e);
        }
    }
    
    private String calculatePackageChecksum(List<SerializableFileData> files) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (SerializableFileData file : files) {
                md.update(file.getChecksum().getBytes());
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating package checksum", e);
        }
    }
    
    public byte[] compressData(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }
    
    public byte[] decompressData(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }
    
    private String extractFilenameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
    
    private String getContentTypeFromMediaType(MultiMedia.MediaType mediaType) {
        return switch (mediaType) {
            case photo -> "image/jpeg";
            case video -> "video/mp4";
            case audio -> "audio/mpeg";
        };
    }
    
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }
} 