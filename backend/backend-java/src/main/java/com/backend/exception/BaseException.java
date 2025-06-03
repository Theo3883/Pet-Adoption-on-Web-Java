package com.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp;
    private final String userMessage;
    private final String developerMessage;

    protected BaseException(String errorCode, HttpStatus httpStatus, String userMessage, String developerMessage,
            Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
        this.developerMessage = developerMessage;
        this.timestamp = LocalDateTime.now();
    }

    protected BaseException(String errorCode, HttpStatus httpStatus, String userMessage, String developerMessage) {
        this(errorCode, httpStatus, userMessage, developerMessage, null);
    }

    protected BaseException(String errorCode, HttpStatus httpStatus, String userMessage) {
        this(errorCode, httpStatus, userMessage, userMessage, null);
    }

    public String getFormattedMessage() {
        return String.format("[%s] %s - %s", errorCode, timestamp, userMessage);
    }

    @Override
    public String toString() {
        return String.format("%s{errorCode='%s', httpStatus=%s, userMessage='%s', timestamp=%s}",
                getClass().getSimpleName(), errorCode, httpStatus, userMessage, timestamp);
    }
}
