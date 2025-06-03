package com.backend.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseException {

    private static final String ERROR_CODE_PREFIX = "RES";

    public ResourceNotFoundException(String message) {
        super(ERROR_CODE_PREFIX + "_404", HttpStatus.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "_404", HttpStatus.NOT_FOUND, message, message, cause);
    }

    public static ResourceNotFoundException userNotFound(Long userId) {
        return new ResourceNotFoundException(String.format("User with ID %d not found", userId));
    }

    public static ResourceNotFoundException animalNotFound(Long animalId) {
        return new ResourceNotFoundException(String.format("Animal with ID %d not found", animalId));
    }

    public static ResourceNotFoundException messageNotFound(Long messageId) {
        return new ResourceNotFoundException(String.format("Message with ID %d not found", messageId));
    }

    public static ResourceNotFoundException adminNotFound(String email) {
        return new ResourceNotFoundException(String.format("Admin with email %s not found", email));
    }

    public static ResourceNotFoundException addressNotFound(Long userId) {
        return new ResourceNotFoundException(String.format("Address for user ID %d not found", userId));
    }

    public static ResourceNotFoundException feedingScheduleNotFound(Long animalId) {
        return new ResourceNotFoundException(String.format("Feeding schedule for animal ID %d not found", animalId));
    }

    public static ResourceNotFoundException medicalHistoryNotFound(Long animalId) {
        return new ResourceNotFoundException(String.format("Medical history for animal ID %d not found", animalId));
    }    public static ResourceNotFoundException multimediaNotFound(Long animalId) {
        return new ResourceNotFoundException(String.format("Multimedia for animal ID %d not found", animalId));
    }

    public static ResourceNotFoundException mediaNotFound(Long mediaId) {
        return new ResourceNotFoundException(String.format("Media with ID %d not found", mediaId));
    }
}
