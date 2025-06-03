package com.backend.dto;

import com.backend.model.Animal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnimalRequest {

    @NotBlank(message = "Animal name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Breed must not exceed 100 characters")
    private String breed;

    @Size(max = 100, message = "Species must not exceed 100 characters")
    private String species;

    private Integer age;

    @NotNull(message = "Gender is required")
    private Animal.Gender gender;
}