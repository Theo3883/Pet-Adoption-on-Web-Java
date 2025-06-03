package com.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedingScheduleResponse {
    private Long id;
    @JsonProperty("FEEDING_TIME")
    private List<String> feedingTime;
    @JsonProperty("FOOD_TYPE")
    private String foodType;
    @JsonProperty("NOTES")
    private String notes;
}