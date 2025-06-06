package com.backend.dto;

import lombok.Data;

@Data
public class UserSignupRequest {
    
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private AddressRequest address;
    
    @Data
    public static class AddressRequest {
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }
} 