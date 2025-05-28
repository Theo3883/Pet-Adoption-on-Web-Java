package com.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    
    private Long userId;
    
    @JsonProperty("FIRSTNAME")
    private String firstName;
    
    @JsonProperty("LASTNAME")
    private String lastName;
    
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private AddressResponse address;
} 