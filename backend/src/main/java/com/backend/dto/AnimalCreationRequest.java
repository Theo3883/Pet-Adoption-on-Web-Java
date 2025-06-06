package com.backend.dto;

import lombok.Data;
import com.backend.model.Animal;

import java.util.List;

@Data
public class AnimalCreationRequest {
    private Long userID;
    private String name;
    private String breed;
    private String species;
    private Integer age;
    private Animal.Gender gender;
    private List<FeedingScheduleCreationRequest> feedingSchedule;
    private List<MedicalHistoryCreationRequest> medicalHistory;
    private List<MultimediaCreationRequest> multimedia;
    private RelationsCreationRequest relations;

    @Data
    public static class FeedingScheduleCreationRequest {
        private String feedingTime;
        private String foodType;
        private String notes;
        private List<String> feedingTimes;
        
        public List<String> getFeedingTimes() {
            if (feedingTimes != null && !feedingTimes.isEmpty()) {
                return feedingTimes;
            }
            if (feedingTime != null && !feedingTime.trim().isEmpty()) {
                return List.of(feedingTime);
            }
            return List.of();
        }
    }

    @Data
    public static class MedicalHistoryCreationRequest {
        private String recordDate;
        private String description;
        private String vetNumber;
        private String first_aid_noted;
    }

    @Data
    public static class MultimediaCreationRequest {
        private String mediaType;
        private String url;
        private String description;
    }

    @Data
    public static class RelationsCreationRequest {
        private String friendWith;
    }
} 