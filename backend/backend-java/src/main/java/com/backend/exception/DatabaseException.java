package com.backend.exception;

import org.springframework.http.HttpStatus;

public class DatabaseException extends BaseException {

    public DatabaseException(String errorCode, String userMessage, String developerMessage) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, userMessage, developerMessage);
    }

    public DatabaseException(String errorCode, String userMessage, String developerMessage, Throwable cause) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, userMessage, developerMessage, cause);
    }

    public static DatabaseException connectionFailure(String details) {
        return new DatabaseException(
                "DB_CONNECTION_FAILURE",
                "Database is temporarily unavailable. Please try again later.",
                "Database connection failure: " + details);
    }

    public static DatabaseException constraintViolation(String constraint, String details) {
        return new DatabaseException(
                "DB_CONSTRAINT_VIOLATION",
                "The operation violates data constraints.",
                "Database constraint violation on " + constraint + ": " + details);
    }

    public static DatabaseException dataIntegrityError(String details) {
        return new DatabaseException(
                "DB_DATA_INTEGRITY_ERROR",
                "Data integrity error occurred. Please check your input.",
                "Data integrity error: " + details);
    }

    public static DatabaseException transactionFailure(String details) {
        return new DatabaseException(
                "DB_TRANSACTION_FAILURE",
                "Transaction failed. Please try again.",
                "Database transaction failure: " + details);
    }

    public static DatabaseException optimisticLockingFailure(String entity) {
        return new DatabaseException(
                "DB_OPTIMISTIC_LOCKING_FAILURE",
                "The data has been modified by another user. Please refresh and try again.",
                "Optimistic locking failure for entity: " + entity);
    }

    public static DatabaseException sqlError(String sql, String error) {
        return new DatabaseException(
                "DB_SQL_ERROR",
                "Database operation failed. Please contact support.",
                "SQL error executing: " + sql + ". Error: " + error);
    }
}
