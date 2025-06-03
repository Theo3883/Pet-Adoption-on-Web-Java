package com.backend.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int status;
    private String error;
    private String message;
    private String developerMessage;
    private String errorCode;
    private String path;
    private List<String> validationErrors;
    private Map<String, Object> additionalInfo;
    
    public ErrorResponse() {}
    
    private ErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.developerMessage = builder.developerMessage;
        this.errorCode = builder.errorCode;
        this.path = builder.path;
        this.validationErrors = builder.validationErrors;
        this.additionalInfo = builder.additionalInfo;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String developerMessage;
        private String errorCode;
        private String path;
        private List<String> validationErrors;
        private Map<String, Object> additionalInfo;
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder status(int status) {
            this.status = status;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder developerMessage(String developerMessage) {
            this.developerMessage = developerMessage;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder path(String path) {
            this.path = path;
            return this;
        }
        
        public Builder validationErrors(List<String> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }
        
        public Builder additionalInfo(Map<String, Object> additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }
        
        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}
