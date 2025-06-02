package com.backend.controller;

import com.backend.service.FileStorageService;
import com.backend.controller.taskstatus.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class FileUploadController {    private final FileStorageService fileStorageService;
    private final ExecutorService fileThreadPool = Executors.newFixedThreadPool(10);    private final Map<String, TaskStatus<String>> uploadTasks = new ConcurrentHashMap<>();
    private final Map<String, TaskStatus<Map<String, Object>>> batchTasks = new ConcurrentHashMap<>();
    private final Map<String, TaskStatus<byte[]>> downloadTasks = new ConcurrentHashMap<>();
    private final Map<String, TaskStatus<Boolean>> deleteTasks = new ConcurrentHashMap<>();

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", defaultValue = "photo") String mediaType) {

        try {
            if (file.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No file uploaded");
                error.put("success", false);
                return ResponseEntity.badRequest().body(error);
            }

            String effectiveMediaType = determineMediaType(file, mediaType);
            log.info("Starting file upload process for {} ({})", file.getOriginalFilename(), effectiveMediaType);

            String filename = fileStorageService.storeFile(file, effectiveMediaType);
            String publicUrl = fileStorageService.getPublicUrl(effectiveMediaType, filename);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filePath", publicUrl);
            response.put("filename", filename);
            response.put("mediaType", effectiveMediaType);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            error.put("success", false);
            return ResponseEntity.status(500).body(error);
        }
    }    @PostMapping("/upload/threaded")
    public ResponseEntity<Map<String, Object>> uploadFileThreaded(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mediaType", defaultValue = "photo") String mediaType) {
        if (file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No file uploaded");
            error.put("success", false);
            return ResponseEntity.badRequest().body(error);
        }
        String trackingId = UUID.randomUUID().toString();
        String effectiveMediaType = determineMediaType(file, mediaType);
        log.info("Starting threaded file upload with tracking ID: {} for file: {} ({} bytes) on thread: {}",
                trackingId, file.getOriginalFilename(), file.getSize(), Thread.currentThread().getName());
        Future<String> uploadFuture = fileThreadPool.submit(() -> {
            try {
                log.info("Processing file upload for tracking ID: {} on worker thread: {}",
                        trackingId, Thread.currentThread().getName());
                return fileStorageService.storeFile(file, effectiveMediaType);
            } catch (IOException e) {
                log.error("Threaded upload failed for tracking ID: {}: {}", trackingId, e.getMessage(), e);
                throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
            }
        });
        uploadTasks.put(trackingId, new TaskStatus<>(uploadFuture, file.getOriginalFilename(), effectiveMediaType, "upload"));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("trackingId", trackingId);
        response.put("status", "processing");
        response.put("message", "File upload started in background");
        response.put("originalFilename", file.getOriginalFilename());
        response.put("mediaType", effectiveMediaType);
        response.put("size", file.getSize());
        response.put("uploadTimestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }    @GetMapping("/upload/status/{trackingId}")
    public ResponseEntity<Map<String, Object>> getUploadStatus(@PathVariable String trackingId) {
        TaskStatus<String> status = uploadTasks.get(trackingId);
        if (status == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Upload task not found");
            error.put("success", false);
            error.put("trackingId", trackingId);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("trackingId", trackingId);
        response.put("mediaType", status.getMediaType());
        response.put("filename", status.getFilename());
        response.put("elapsedTime", status.getElapsedTime());
        
        if (status.isDone()) {
            try {
                String filename = status.getFuture().get();
                uploadTasks.remove(trackingId);
                String publicUrl = fileStorageService.getPublicUrl(status.getMediaType(), filename);
                response.put("success", true);
                response.put("status", "completed");
                response.put("result", filename);
                response.put("filePath", publicUrl);
                response.put("completedTimestamp", System.currentTimeMillis());
                log.info("Upload completed for tracking ID: {}", trackingId);
            } catch (ExecutionException e) {
                uploadTasks.remove(trackingId);
                response.put("success", false);
                response.put("status", "failed");
                response.put("error", e.getCause().getMessage());
                response.put("failedTimestamp", System.currentTimeMillis());
                log.error("Upload failed for tracking ID: {}: {}", trackingId, e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.put("success", false);
                response.put("status", "interrupted");
                response.put("error", "Upload was interrupted");
            }
        } else if (status.isCancelled()) {
            uploadTasks.remove(trackingId);
            response.put("success", false);
            response.put("status", "cancelled");
            response.put("error", "Upload was cancelled");
        } else {
            response.put("success", true);
            response.put("status", "processing");
            response.put("message", "Upload still in progress");
        }
        return ResponseEntity.ok(response);
    }    @PostMapping("/upload/batch")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "mediaType", defaultValue = "photo") String mediaType) {

        String batchId = UUID.randomUUID().toString();
        log.info("Starting threaded batch upload with ID: {} for {} files on thread: {}", 
                batchId, files.length, Thread.currentThread().getName());

        if (files.length == 0) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No files uploaded");
            error.put("success", false);
            return ResponseEntity.badRequest().body(error);
        }        // Submit batch processing task to thread pool
        Future<Map<String, Object>> batchFuture = fileThreadPool.submit(() -> {
            return processBatchUploadInBackground(files, mediaType, batchId);
        });

        // Store the batch task with a special TaskStatus for batch operations
        batchTasks.put(batchId, new TaskStatus<>(batchFuture, "batch-" + files.length + "-files", mediaType, "batch-upload"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("batchId", batchId);
        response.put("status", "processing");
        response.put("message", "Batch upload started in background");
        response.put("totalFiles", files.length);
        response.put("uploadTimestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }    private Map<String, Object> processBatchUploadInBackground(
            MultipartFile[] files, String mediaType, String batchId) {
        
        Map<String, Object> fileResults = new HashMap<>();
        
        for (int i = 0; i < files.length; i++) {
            try {
                MultipartFile file = files[i];
                String effectiveMediaType = determineMediaType(file, mediaType);
                
                log.info("Processing file {} of {} in batch {} on thread: {}", 
                        i + 1, files.length, batchId, Thread.currentThread().getName());
                
                String filename = fileStorageService.storeFile(file, effectiveMediaType);
                
                // Build success response for individual file
                Map<String, Object> fileInfo = new HashMap<>();
                String publicUrl = fileStorageService.getPublicUrl(effectiveMediaType, filename);
                fileInfo.put("success", true);
                fileInfo.put("filePath", publicUrl);
                fileInfo.put("filename", filename);
                fileInfo.put("originalFilename", file.getOriginalFilename());
                fileInfo.put("mediaType", effectiveMediaType);
                fileInfo.put("size", file.getSize());
                fileResults.put("file" + i, fileInfo);
                
            } catch (Exception e) {
                log.error("File upload failed in batch {} for file {}: {}", 
                        batchId, files[i].getOriginalFilename(), e.getMessage());
                
                // Build error response for individual file
                Map<String, Object> fileError = new HashMap<>();
                fileError.put("success", false);
                fileError.put("error", "Failed to upload: " + e.getMessage());
                fileError.put("originalFilename", files[i].getOriginalFilename());
                fileError.put("size", files[i].getSize());
                fileResults.put("file" + i, fileError);
            }
        }

        Map<String, Object> batchResult = new HashMap<>();
        batchResult.put("success", true);
        batchResult.put("batchId", batchId);
        batchResult.put("totalFiles", files.length);
        batchResult.put("files", fileResults);
        batchResult.put("completedTimestamp", System.currentTimeMillis());
        
        log.info("Batch upload completed with ID: {} for {} files on thread: {}", 
                batchId, files.length, Thread.currentThread().getName());
        
        return batchResult;
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
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(fileContent);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error loading file: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }    @GetMapping("/server/threaded/{mediaType}/{filename}")
    public ResponseEntity<Map<String, Object>> getMediaThreaded(
            @PathVariable String mediaType,
            @PathVariable String filename) {

        if (!fileStorageService.fileExists(mediaType, filename)) {
            return ResponseEntity.notFound().build();
        }

        String trackingId = UUID.randomUUID().toString();
        log.info("Starting threaded file retrieval with tracking ID: {} for {}/{} on thread: {}", 
                trackingId, mediaType, filename, Thread.currentThread().getName());

        // Submit download task to thread pool
        Future<byte[]> downloadFuture = fileThreadPool.submit(() -> {
            try {
                log.info("Processing file download for tracking ID: {} on worker thread: {}", 
                        trackingId, Thread.currentThread().getName());
                return fileStorageService.loadFile(mediaType, filename);
            } catch (IOException e) {
                log.error("Threaded download failed for tracking ID: {}: {}", trackingId, e.getMessage(), e);
                throw new RuntimeException("Failed to load file: " + e.getMessage(), e);
            }
        });

        downloadTasks.put(trackingId, new TaskStatus<>(downloadFuture, filename, mediaType, "download"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("trackingId", trackingId);
        response.put("status", "processing");
        response.put("message", "File download started in background");
        response.put("mediaType", mediaType);
        response.put("filename", filename);
        response.put("downloadTimestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }    @GetMapping("/download/status/{trackingId}")
    public ResponseEntity<Map<String, Object>> getDownloadStatus(@PathVariable String trackingId) {
        TaskStatus<byte[]> downloadStatus = downloadTasks.get(trackingId);
        
        if (downloadStatus == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Download task not found");
            error.put("success", false);
            error.put("trackingId", trackingId);
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("trackingId", trackingId);
        response.put("mediaType", downloadStatus.getMediaType());
        response.put("filename", downloadStatus.getFilename());
        response.put("elapsedTime", downloadStatus.getElapsedTime());

        if (downloadStatus.isDone()) {
            try {
                byte[] fileContent = downloadStatus.getFuture().get();
                downloadTasks.remove(trackingId); // Clean up completed task
                
                response.put("success", true);
                response.put("status", "completed");
                response.put("contentLength", fileContent.length);
                response.put("completedTimestamp", System.currentTimeMillis());
                
                log.info("Download completed for tracking ID: {}", trackingId);
                
            } catch (ExecutionException e) {
                downloadTasks.remove(trackingId); // Clean up failed task
                response.put("success", false);
                response.put("status", "failed");
                response.put("error", e.getCause().getMessage());
                response.put("failedTimestamp", System.currentTimeMillis());
                
                log.error("Download failed for tracking ID: {}: {}", trackingId, e.getCause().getMessage());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.put("success", false);
                response.put("status", "interrupted");
                response.put("error", "Download was interrupted");
            }
        } else if (downloadStatus.isCancelled()) {
            downloadTasks.remove(trackingId);
            response.put("success", false);
            response.put("status", "cancelled");
            response.put("error", "Download was cancelled");
        } else {
            response.put("success", true);
            response.put("status", "processing");
            response.put("message", "Download still in progress");
        }

        return ResponseEntity.ok(response);
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
    }       @DeleteMapping("/server/threaded/{mediaType}/{filename}")
    public ResponseEntity<Map<String, Object>> deleteMediaThreaded(
            @PathVariable String mediaType,
            @PathVariable String filename) {

        String trackingId = UUID.randomUUID().toString();
        log.info("Starting threaded file deletion with tracking ID: {} for {}/{} on thread: {}", 
                trackingId, mediaType, filename, Thread.currentThread().getName());

        // Submit delete task to thread pool
        Future<Boolean> deleteFuture = fileThreadPool.submit(() -> {
            try {
                log.info("Processing file deletion for tracking ID: {} on worker thread: {}", 
                        trackingId, Thread.currentThread().getName());
                return fileStorageService.deleteFile(mediaType, filename);
            } catch (Exception e) {
                log.error("Threaded deletion failed for tracking ID: {}: {}", trackingId, e.getMessage(), e);
                throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
            }
        });

        deleteTasks.put(trackingId, new TaskStatus<>(deleteFuture, filename, mediaType, "delete"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("trackingId", trackingId);
        response.put("status", "processing");
        response.put("message", "File deletion started in background");
        response.put("mediaType", mediaType);
        response.put("filename", filename);
        response.put("deleteTimestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }    @GetMapping("/delete/status/{trackingId}")
    public ResponseEntity<Map<String, Object>> getDeleteStatus(@PathVariable String trackingId) {
        TaskStatus<Boolean> deleteStatus = deleteTasks.get(trackingId);
        
        if (deleteStatus == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Delete task not found");
            error.put("success", false);
            error.put("trackingId", trackingId);
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("trackingId", trackingId);
        response.put("mediaType", deleteStatus.getMediaType());
        response.put("filename", deleteStatus.getFilename());
        response.put("elapsedTime", deleteStatus.getElapsedTime());

        if (deleteStatus.isDone()) {
            try {
                Boolean deleted = deleteStatus.getFuture().get();
                deleteTasks.remove(trackingId); // Clean up completed task
                
                if (deleted) {
                    response.put("success", true);
                    response.put("status", "completed");
                    response.put("message", "File deleted successfully");
                    response.put("completedTimestamp", System.currentTimeMillis());
                } else {
                    response.put("success", false);
                    response.put("status", "completed");
                    response.put("message", "File not found");
                    response.put("completedTimestamp", System.currentTimeMillis());
                }
                
                log.info("Delete completed for tracking ID: {}, result: {}", trackingId, deleted);
                
            } catch (ExecutionException e) {
                deleteTasks.remove(trackingId); // Clean up failed task
                response.put("success", false);
                response.put("status", "failed");
                response.put("error", e.getCause().getMessage());
                response.put("failedTimestamp", System.currentTimeMillis());
                
                log.error("Delete failed for tracking ID: {}: {}", trackingId, e.getCause().getMessage());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.put("success", false);
                response.put("status", "interrupted");
                response.put("error", "Delete was interrupted");
            }
        } else if (deleteStatus.isCancelled()) {
            deleteTasks.remove(trackingId);
            response.put("success", false);
            response.put("status", "cancelled");
            response.put("error", "Delete was cancelled");
        } else {
            response.put("success", true);
            response.put("status", "processing");
            response.put("message", "Delete still in progress");
        }

        return ResponseEntity.ok(response);
    }private String determineMediaType(MultipartFile file, String defaultMediaType) {
        if ("photo".equals(defaultMediaType)) {
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("video/")) {
                    return "video";
                } else if (contentType.startsWith("audio/")) {
                    return "audio";
                }
            }
        }
        return defaultMediaType;
    }

    @GetMapping("/upload/batch/status/{batchId}")
    public ResponseEntity<Map<String, Object>> getBatchUploadStatus(@PathVariable String batchId) {
        TaskStatus<Map<String, Object>> batchStatus = batchTasks.get(batchId);
        if (batchStatus == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Batch upload task not found");
            error.put("success", false);
            error.put("batchId", batchId);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchId", batchId);
        response.put("mediaType", batchStatus.getMediaType());
        response.put("filename", batchStatus.getFilename());
        response.put("elapsedTime", batchStatus.getElapsedTime());
        
        if (batchStatus.isDone()) {
            try {
                Map<String, Object> batchResult = batchStatus.getFuture().get();
                batchTasks.remove(batchId);
                response.put("success", true);
                response.put("status", "completed");
                response.putAll(batchResult); // Merge batch results into response
                log.info("Batch upload completed for batch ID: {}", batchId);
            } catch (ExecutionException e) {
                batchTasks.remove(batchId);
                response.put("success", false);
                response.put("status", "failed");
                response.put("error", e.getCause().getMessage());
                response.put("failedTimestamp", System.currentTimeMillis());
                log.error("Batch upload failed for batch ID: {}: {}", batchId, e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.put("success", false);
                response.put("status", "interrupted");
                response.put("error", "Batch upload was interrupted");
            }
        } else if (batchStatus.isCancelled()) {
            batchTasks.remove(batchId);
            response.put("success", false);
            response.put("status", "cancelled");
            response.put("error", "Batch upload was cancelled");
        } else {
            response.put("success", true);
            response.put("status", "processing");
            response.put("message", "Batch upload still in progress");
        }
        return ResponseEntity.ok(response);
    }
}