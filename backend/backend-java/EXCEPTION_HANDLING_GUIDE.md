# Exception Handling System Documentation

## Overview

This document describes the comprehensive exception handling system implemented in the Pet Adoption Web Application. The system provides centralized, consistent error handling across all endpoints using custom exception classes and a global exception handler.

## Architecture

### Exception Hierarchy

```
BaseException (Abstract)
├── AuthenticationException
├── AuthorizationException
├── ValidationException
├── ResourceNotFoundException
├── FileException
├── BusinessLogicException
├── DatabaseException
└── ServiceException
```

### Core Components

1. **BaseException** - Abstract base class for all custom exceptions
2. **GlobalExceptionHandler** - Centralized exception handling with @RestControllerAdvice
3. **ErrorResponse** - Standardized error response format
4. **Custom Exception Classes** - Specific exception types for different error scenarios

## Error Response Format

All API errors return a consistent JSON format:

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "timestamp": "2024-01-15T10:30:00Z",
  "httpStatus": 404,
  "userMessage": "The requested resource was not found",
  "developerMessage": "User with ID 123 not found in database",
  "validationErrors": {
    "fieldName": ["Field-specific error message"]
  }
}
```

### Response Fields

- **errorCode**: Unique identifier for the error type
- **timestamp**: ISO 8601 formatted timestamp when the error occurred
- **httpStatus**: HTTP status code
- **userMessage**: User-friendly error message
- **developerMessage**: Technical details for developers
- **validationErrors**: Field-specific validation errors (optional)

## Exception Classes

### AuthenticationException

Used for authentication-related errors.

**HTTP Status**: 401 Unauthorized

**Static Methods**:
- `authenticationRequired()` - When authentication is required but not provided
- `invalidCredentials()` - When login credentials are invalid
- `tokenExpired()` - When JWT token has expired
- `tokenInvalid()` - When JWT token is malformed or invalid

**Example Usage**:
```java
if (userId == null) {
    throw AuthenticationException.authenticationRequired();
}
```

### AuthorizationException

Used for authorization/permission-related errors.

**HTTP Status**: 403 Forbidden

**Static Methods**:
- `accessDenied(String resource)` - When user lacks permission for a resource
- `roleRequired(String role)` - When a specific role is required
- `ownershipRequired(String resource)` - When user must own the resource

### ValidationException

Used for input validation errors.

**HTTP Status**: 400 Bad Request

**Static Methods**:
- `missingRequiredField(String fieldName)` - When required field is missing
- `invalidValue(String fieldName, String reason)` - When field value is invalid
- `duplicateValue(String fieldName, String value)` - When value already exists
- `emailAlreadyExists()` - When email is already registered
- `invalidEmail()` - When email format is invalid
- `passwordTooWeak()` - When password doesn't meet requirements

### ResourceNotFoundException

Used when requested resources don't exist.

**HTTP Status**: 404 Not Found

**Static Methods**:
- `userNotFound(Long userId)` - When user doesn't exist
- `animalNotFound(Long animalId)` - When animal doesn't exist
- `fileNotFound(String fileName)` - When file doesn't exist
- `custom(String resourceType, String identifier)` - For custom resources

### FileException

Used for file operation errors.

**HTTP Status**: 422 Unprocessable Entity (or 400 for format issues)

**Static Methods**:
- `fileNotFound(String fileName)` - When file doesn't exist
- `invalidFileFormat(String fileName, String expectedFormat)` - When file format is wrong
- `fileSizeTooLarge(String fileName, long maxSize)` - When file exceeds size limit
- `fileAccessDenied(String fileName)` - When file access is denied
- `fileCorrupted(String fileName)` - When file is corrupted

### BusinessLogicException

Used for business rule violations.

**HTTP Status**: 422 Unprocessable Entity

**Static Methods**:
- `animalNotAvailable(Long animalId)` - When animal is not available for adoption
- `insufficientPrivileges(String action)` - When user lacks privileges for action
- `operationNotAllowed(String operation, String reason)` - When operation violates business rules

### DatabaseException

Used for database operation errors.

**HTTP Status**: 500 Internal Server Error

**Static Methods**:
- `connectionFailure(String details)` - When database connection fails
- `queryExecutionFailure(String query, String details)` - When query execution fails
- `transactionFailure(String operation, String details)` - When transaction fails
- `dataIntegrityViolation(String details)` - When data integrity is violated

### ServiceException

Used for external service errors.

**HTTP Status**: 503 Service Unavailable (configurable)

**Static Methods**:
- `serviceUnavailable(String serviceName)` - When external service is unavailable
- `externalServiceFailure(String serviceName, String details)` - When external service fails
- `configurationError(String configItem, String details)` - When configuration is wrong
- `timeoutError(String operation, long timeoutMs)` - When operation times out
- `rateLimitExceeded(String details)` - When rate limit is exceeded
- `fileProcessingError(String fileName, String details)` - When file processing fails
- `emailServiceError(String details)` - When email service fails

## Controller Implementation

### Before (Old Approach)

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
    try {
        if (request.getEmail() == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        User user = userService.createUser(request);
        return ResponseEntity.ok(user);
    } catch (Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        return ResponseEntity.status(500).body(error);
    }
}
```

### After (New Approach)

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
    if (request.getEmail() == null) {
        throw ValidationException.missingRequiredField("email");
    }
    
    User user = userService.createUser(request);
    return ResponseEntity.ok(user);
}
```

## Service Implementation

### Before (Old Approach)

```java
public User findUserById(Long userId) {
    Optional<User> user = userRepository.findById(userId);
    if (user.isEmpty()) {
        throw new RuntimeException("User not found");
    }
    return user.get();
}
```

### After (New Approach)

```java
public User findUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> ResourceNotFoundException.userNotFound(userId));
}
```

## Benefits

### 1. Consistency
- All errors follow the same response format
- Consistent error codes and messages
- Unified handling across all endpoints

### 2. Maintainability
- Centralized error handling logic
- Easy to modify error responses globally
- Reduced code duplication

### 3. Developer Experience
- Clear, informative error messages
- Structured error codes for programmatic handling
- Comprehensive documentation

### 4. User Experience
- User-friendly error messages
- Consistent error presentation
- Proper HTTP status codes

### 5. Debugging
- Detailed developer messages
- Timestamps for error tracking
- Stack trace information in development

## Best Practices

### 1. Use Specific Exception Types
```java
// Good
throw ResourceNotFoundException.userNotFound(userId);

// Avoid
throw new RuntimeException("User not found");
```

### 2. Provide Context in Error Messages
```java
// Good
throw ValidationException.invalidValue("age", "must be between 0 and 150");

// Avoid
throw ValidationException.missingRequiredField("field");
```

### 3. Use Static Factory Methods
```java
// Good
throw AuthenticationException.authenticationRequired();

// Avoid
throw new AuthenticationException("AUTH_REQUIRED", "Authentication required", "...");
```

### 4. Let Global Handler Manage Responses
```java
// Good - Let GlobalExceptionHandler handle the response
throw ValidationException.missingRequiredField("email");

// Avoid - Manual ResponseEntity creation
return ResponseEntity.badRequest().body(errorMap);
```

### 5. Remove Try-Catch from Controllers
Controllers should focus on business logic and let exceptions bubble up to the global handler.

## Error Code Reference

| Error Code | Exception Type | HTTP Status | Description |
|------------|---------------|-------------|-------------|
| AUTH_REQUIRED | AuthenticationException | 401 | Authentication required |
| INVALID_CREDENTIALS | AuthenticationException | 401 | Invalid login credentials |
| TOKEN_EXPIRED | AuthenticationException | 401 | JWT token expired |
| TOKEN_INVALID | AuthenticationException | 401 | JWT token invalid |
| ACCESS_DENIED | AuthorizationException | 403 | Access denied |
| ROLE_REQUIRED | AuthorizationException | 403 | Specific role required |
| OWNERSHIP_REQUIRED | AuthorizationException | 403 | Resource ownership required |
| MISSING_REQUIRED_FIELD | ValidationException | 400 | Required field missing |
| INVALID_VALUE | ValidationException | 400 | Field value invalid |
| DUPLICATE_VALUE | ValidationException | 400 | Value already exists |
| EMAIL_ALREADY_EXISTS | ValidationException | 400 | Email already registered |
| INVALID_EMAIL | ValidationException | 400 | Email format invalid |
| PASSWORD_TOO_WEAK | ValidationException | 400 | Password too weak |
| RESOURCE_NOT_FOUND | ResourceNotFoundException | 404 | Resource not found |
| USER_NOT_FOUND | ResourceNotFoundException | 404 | User not found |
| ANIMAL_NOT_FOUND | ResourceNotFoundException | 404 | Animal not found |
| FILE_NOT_FOUND | FileException | 404 | File not found |
| INVALID_FILE_FORMAT | FileException | 400 | Invalid file format |
| FILE_SIZE_TOO_LARGE | FileException | 413 | File size exceeds limit |
| FILE_ACCESS_DENIED | FileException | 403 | File access denied |
| FILE_CORRUPTED | FileException | 422 | File corrupted |
| ANIMAL_NOT_AVAILABLE | BusinessLogicException | 422 | Animal not available |
| INSUFFICIENT_PRIVILEGES | BusinessLogicException | 422 | Insufficient privileges |
| OPERATION_NOT_ALLOWED | BusinessLogicException | 422 | Operation not allowed |
| DATABASE_CONNECTION_FAILURE | DatabaseException | 500 | Database connection failed |
| QUERY_EXECUTION_FAILURE | DatabaseException | 500 | Query execution failed |
| TRANSACTION_FAILURE | DatabaseException | 500 | Transaction failed |
| DATA_INTEGRITY_VIOLATION | DatabaseException | 500 | Data integrity violated |
| SERVICE_UNAVAILABLE | ServiceException | 503 | External service unavailable |
| EXTERNAL_SERVICE_FAILURE | ServiceException | 503 | External service failed |
| SERVICE_CONFIGURATION_ERROR | ServiceException | 500 | Service configuration error |
| SERVICE_TIMEOUT | ServiceException | 408 | Service timeout |
| RATE_LIMIT_EXCEEDED | ServiceException | 429 | Rate limit exceeded |
| FILE_PROCESSING_ERROR | ServiceException | 422 | File processing error |
| EMAIL_SERVICE_ERROR | ServiceException | 503 | Email service error |

## Testing

The exception handling system is automatically tested through the existing test suite. All controllers now use the new exception handling approach, and the global exception handler ensures consistent error responses.

## Migration Complete

The following components have been successfully migrated:

### Controllers Updated:
- ✅ UserController
- ✅ AdminController  
- ✅ AnimalController
- ✅ MessageController
- ✅ NewsletterController
- ✅ FileUploadController

### Services Updated:
- ✅ UserService
- ✅ AdminService
- ✅ MessageService
- ✅ AnimalService
- ✅ NewsletterService
- ✅ CrossPlatformFileExchangeService

### Exception Infrastructure:
- ✅ BaseException abstract class
- ✅ All custom exception classes
- ✅ GlobalExceptionHandler
- ✅ ErrorResponse model
- ✅ Static factory methods for common errors

All scattered try-catch blocks and RuntimeExceptions have been successfully replaced with the centralized exception handling system.
