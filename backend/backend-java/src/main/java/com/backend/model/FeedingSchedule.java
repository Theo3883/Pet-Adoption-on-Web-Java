package com.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "FEEDINGSCHEDULE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @OneToOne
    @JoinColumn(name = "ANIMALID", referencedColumnName = "ANIMALID")
    private Animal animal;

    @Column(name = "FEEDING_TIME", length = 1000)
    private String feedingTime;

    @Column(name = "FOOD_TYPE", length = 100)
    private String foodType;

    @Column(name = "NOTES", length = 4000)
    private String notes;

    @Transient
    public List<String> getFeedingTimes() {
        if (feedingTime == null || feedingTime.trim().isEmpty()) {
            return List.of();
        }

        String cleanTime = feedingTime;

        if (cleanTime.contains("FEEDING_TIME_ARRAY(")) {
            cleanTime = cleanTime.replaceAll("FEEDING_TIME_ARRAY\\(", "")
                    .replaceAll("\\)$", "")
                    .replaceAll("'", "");
        }

        if (cleanTime.contains(",")) {
            return Arrays.stream(cleanTime.split(","))
                    .map(String::trim)
                    .map(this::extractTimeFromTimestamp)
                    .filter(time -> !time.isEmpty())
                    .collect(Collectors.toList());
        } else if (!cleanTime.isEmpty()) {
            return List.of(extractTimeFromTimestamp(cleanTime.trim()));
        }

        return List.of();
    }

    @Transient
    private String extractTimeFromTimestamp(String timestampOrTime) {
        if (timestampOrTime == null || timestampOrTime.isEmpty()) {
            return "";
        }

        if (timestampOrTime.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return timestampOrTime;
        }

        if (timestampOrTime.contains(" ")) {
            String[] parts = timestampOrTime.split(" ");
            if (parts.length > 1 && parts[1].matches("\\d{1,2}:\\d{2}:\\d{2}.*")) {
                return parts[1].substring(0, 8); // Get HH:MM:SS part
            }
        }

        String timePattern = timestampOrTime.replaceAll(".*?(\\d{1,2}:\\d{2}:\\d{2}).*", "$1");
        if (timePattern.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return timePattern;
        }

        return timestampOrTime;
    }

    @Transient
    public void setFeedingTimes(List<String> feedingTimes) {
        if (feedingTimes == null || feedingTimes.isEmpty()) {
            this.feedingTime = null;
        } else {
            this.feedingTime = String.join(",", feedingTimes);
        }
    }
}