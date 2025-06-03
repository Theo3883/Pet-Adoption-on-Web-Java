package com.backend.dto;

import com.backend.model.Animal;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnimalResponse {

    @JsonProperty("ANIMALID")
    private Long animalId;

    @JsonProperty("NAME")
    private String name;

    @JsonProperty("BREED")
    private String breed;

    @JsonProperty("SPECIES")
    private String species;

    @JsonProperty("AGE")
    private Integer age;

    @JsonProperty("VIEWS")
    private Integer views;

    @JsonProperty("GENDER")
    private Animal.Gender gender;

    @JsonProperty("CREATEDAT")
    private LocalDateTime createdAt;

    @JsonProperty("USERID")
    private Long userId;

    private UserResponse user;
    private List<MultiMediaResponse> multimedia;
}