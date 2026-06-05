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
public class RecommendationResponse {
    private List<RecommendedActivity> activities;
    private List<String> reasons;
    private Integer totalFound;
}
