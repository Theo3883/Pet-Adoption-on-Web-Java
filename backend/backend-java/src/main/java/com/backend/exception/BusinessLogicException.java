package com.backend.exception;

import org.springframework.http.HttpStatus;

public class BusinessLogicException extends BaseException {
    
    private static final String ERROR_CODE_PREFIX = "BIZ";
    
    public BusinessLogicException(String message) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
    
    public BusinessLogicException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "_001", HttpStatus.UNPROCESSABLE_ENTITY, message, message, cause);
    }
    
    public static BusinessLogicException animalAlreadyAdopted(Long animalId) {
        return new BusinessLogicException(String.format("Animal with ID %d is already adopted", animalId));
    }
    
    public static BusinessLogicException cannotDeleteAnimalWithActiveAdoptions(Long animalId) {
        return new BusinessLogicException(String.format("Cannot delete animal with ID %d - active adoptions exist", animalId));
    }
    
    public static BusinessLogicException invalidAnimalAge(int age) {
        return new BusinessLogicException(String.format("Invalid animal age: %d. Age must be between 0 and 30 years", age));
    }
    
    public static BusinessLogicException cannotMessageYourself() {
        return new BusinessLogicException("You cannot send messages to yourself");
    }
    
    public static BusinessLogicException feedingTimeOutOfRange() {
        return new BusinessLogicException("Feeding times must be valid 24-hour format (00:00-23:59)");
    }
    
    public static BusinessLogicException tooManyFeedingTimes() {
        return new BusinessLogicException("Cannot have more than 10 feeding times per day");
    }
    
    public static BusinessLogicException invalidSpecies(String species) {
        return new BusinessLogicException(String.format("Species '%s' is not supported", species));
    }
    
    public static BusinessLogicException invalidGender(String gender) {
        return new BusinessLogicException(String.format("Gender '%s' is not valid. Must be Male or Female", gender));
    }
    
    public static BusinessLogicException fileUploadLimitExceeded() {
        return new BusinessLogicException("File upload limit exceeded. Maximum 10 files per animal");
    }
    
    public static BusinessLogicException unsupportedFileType(String fileType) {
        return new BusinessLogicException(String.format("File type '%s' is not supported", fileType));
    }
}
