package com.backend.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {

    private static final String ERROR_CODE_PREFIX = "VAL";

    public ValidationException(String message) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.BAD_REQUEST, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.BAD_REQUEST, message, message, cause);
    }

    public static ValidationException missingRequiredField(String fieldName) {
        return new ValidationException(String.format("Required field '%s' is missing or empty", fieldName));
    }

    public static ValidationException invalidFormat(String fieldName) {
        return new ValidationException(String.format("Field '%s' has invalid format", fieldName));
    }

    public static ValidationException invalidValue(String fieldName, String value) {
        return new ValidationException(String.format("Field '%s' has invalid value: %s", fieldName, value));
    }

    public static ValidationException invalidRange(String fieldName, Object min, Object max) {
        return new ValidationException(String.format("Field '%s' must be between %s and %s", fieldName, min, max));
    }

    public static ValidationException duplicateValue(String fieldName, String value) {
        return new ValidationException(String.format("Value '%s' for field '%s' already exists", value, fieldName));
    }

    public static ValidationException emailAlreadyExists() {
        return new ValidationException("Email address is already registered");
    }

    public static ValidationException invalidEmail() {
        return new ValidationException("Email address format is invalid");
    }

    public static ValidationException passwordTooWeak() {
        return new ValidationException("Password must be at least 8 characters long and contain letters and numbers");
    }
}
