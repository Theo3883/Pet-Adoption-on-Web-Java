package com.backend.exception;

import org.springframework.http.HttpStatus;

public class FileException extends BaseException {
    
    private static final String ERROR_CODE_PREFIX = "FILE";
    
    public FileException(String message) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
    
    public FileException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.INTERNAL_SERVER_ERROR, message, message, cause);
    }
    
    public static FileException fileNotFound(String fileName) {
        return new FileException(String.format("File '%s' not found", fileName));
    }
    
    public static FileException fileUploadFailed(String fileName) {
        return new FileException(String.format("Failed to upload file '%s'", fileName));
    }
    
    public static FileException fileDeleteFailed(String fileName) {
        return new FileException(String.format("Failed to delete file '%s'", fileName));
    }
    
    public static FileException invalidFileType(String fileName, String expectedType) {
        return new FileException(String.format("File '%s' has invalid type. Expected: %s", fileName, expectedType));
    }
    
    public static FileException fileSizeTooLarge(String fileName, long maxSize) {
        return new FileException(String.format("File '%s' exceeds maximum size of %d bytes", fileName, maxSize));
    }
    
    public static FileException fileAccessDenied(String fileName) {
        return new FileException(String.format("Access denied to file '%s'", fileName));
    }
    
    public static FileException fileCorrupted(String fileName) {
        return new FileException(String.format("File '%s' is corrupted or unreadable", fileName));
    }
}
