package com.example.activityservice.feature.activities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedActivity {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String startDate;
    private String endDate;
    private Integer maxParticipants;
    private Integer registeredCount;
    private Double matchPercentage;
    private List<String> matchedReasons;
    private String categoryName;
    private String departmentName;
}
