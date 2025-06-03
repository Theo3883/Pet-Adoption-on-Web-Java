package com.backend.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationException extends BaseException {

    private static final String ERROR_CODE_PREFIX = "AUTHZ";

    public AuthorizationException(String message) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.FORBIDDEN, message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.FORBIDDEN, message, message, cause);
    }

    public static AuthorizationException accessDenied() {
        return new AuthorizationException("Access denied");
    }

    public static AuthorizationException insufficientPermissions() {
        return new AuthorizationException("Insufficient permissions to perform this action");
    }

    public static AuthorizationException adminRequired() {
        return new AuthorizationException("Admin privileges required");
    }

    public static AuthorizationException ownershipRequired() {
        return new AuthorizationException("You can only access your own resources");
    }
}
