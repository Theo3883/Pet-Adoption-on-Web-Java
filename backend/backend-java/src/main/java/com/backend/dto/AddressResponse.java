package com.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AddressResponse {

    private Long addressId;
    private String street;

    @JsonProperty("CITY")
    private String city;

    @JsonProperty("COUNTY")
    private String state;

    private Integer zipCode;
    private String country;
}