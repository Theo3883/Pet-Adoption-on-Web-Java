package com.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryResponse {
    private Long id;

    @JsonProperty("VETNUMBER")
    private String vetNumber;

    @JsonProperty("RECORDDATE")
    private LocalDate recordDate;

    @JsonProperty("DESCRIPTION")
    private String description;

    @JsonProperty("FIRST_AID_NOTED")
    private String firstAidNoted;
}