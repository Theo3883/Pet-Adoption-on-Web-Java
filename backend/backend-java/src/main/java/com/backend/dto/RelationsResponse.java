package com.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationsResponse {
    private Long id;
    
    @JsonProperty("FRIENDWITH")
    private String friendWith;
} 