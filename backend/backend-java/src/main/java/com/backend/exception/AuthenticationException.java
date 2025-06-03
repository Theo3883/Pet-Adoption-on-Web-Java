package com.backend.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {

    private static final String ERROR_CODE_PREFIX = "AUTH";

    public AuthenticationException(String message) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.UNAUTHORIZED, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.UNAUTHORIZED, message, message, cause);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Authentication token has expired");
    }

    public static AuthenticationException tokenInvalid() {
        return new AuthenticationException("Invalid authentication token");
    }

    public static AuthenticationException authenticationRequired() {
        return new AuthenticationException("Authentication is required to access this resource");
    }

    public static AuthenticationException adminPrivilegesRequired() {
        return new AuthenticationException("Admin privileges are required to access this resource");
    }
}
