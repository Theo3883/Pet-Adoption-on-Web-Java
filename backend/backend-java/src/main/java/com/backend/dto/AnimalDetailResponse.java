package com.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnimalDetailResponse {
    private AnimalResponse animal;
    private List<MultiMediaResponse> multimedia;
    private List<FeedingScheduleResponse> feedingSchedule;
    private List<MedicalHistoryResponse> medicalHistory;
    private UserResponse owner;
    private List<AddressResponse> address;
    private List<RelationsResponse> relations;
}