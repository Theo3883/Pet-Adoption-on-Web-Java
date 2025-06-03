package com.backend.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends BaseException {

    public ServiceException(String errorCode, String userMessage, String developerMessage) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, userMessage, developerMessage);
    }

    public ServiceException(String errorCode, String userMessage, String developerMessage, Throwable cause) {
        super(errorCode, HttpStatus.SERVICE_UNAVAILABLE, userMessage, developerMessage, cause);
    }

    public ServiceException(String errorCode, HttpStatus httpStatus, String userMessage, String developerMessage) {
        super(errorCode, httpStatus, userMessage, developerMessage);
    }

    public ServiceException(String errorCode, HttpStatus httpStatus, String userMessage, String developerMessage, Throwable cause) {
        super(errorCode, httpStatus, userMessage, developerMessage, cause);
    }

    public static ServiceException serviceUnavailable(String serviceName) {
        return new ServiceException(
            "SERVICE_UNAVAILABLE",
            "Service is temporarily unavailable. Please try again later.",
            serviceName + " service is currently unavailable"
        );
    }

    public static ServiceException externalServiceFailure(String serviceName, String details) {
        return new ServiceException(
            "EXTERNAL_SERVICE_FAILURE",
            "External service error. Please try again later.",
            "External service failure: " + serviceName + ". Details: " + details
        );
    }

    public static ServiceException configurationError(String configItem, String details) {
        return new ServiceException(
            "SERVICE_CONFIGURATION_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Service configuration error. Please contact support.",
            "Configuration error for " + configItem + ": " + details
        );
    }

    public static ServiceException timeoutError(String operation, long timeoutMs) {
        return new ServiceException(
            "SERVICE_TIMEOUT",
            HttpStatus.REQUEST_TIMEOUT,
            "Request timeout. Please try again.",
            "Operation timeout: " + operation + " (timeout: " + timeoutMs + "ms)"
        );
    }

    public static ServiceException rateLimitExceeded(String details) {
        return new ServiceException(
            "RATE_LIMIT_EXCEEDED",
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many requests. Please wait before trying again.",
            "Rate limit exceeded: " + details
        );
    }

    public static ServiceException fileProcessingError(String fileName, String details) {
        return new ServiceException(
            "FILE_PROCESSING_ERROR",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "File processing failed. Please check the file format.",
            "File processing error for " + fileName + ": " + details
        );
    }

    public static ServiceException emailServiceError(String details) {
        return new ServiceException(
            "EMAIL_SERVICE_ERROR",
            "Email could not be sent. Please try again later.",
            "Email service error: " + details
        );
    }
}
