package com.example.activityservice.feature.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointContributionDetail {
    private String sourceType;
    private Long activityId;
    private String activityTitle;
    private Long certificateSubmissionId;
    private String certificateTitle;
    private Long categoryId;
    private String categoryName;
    private Integer earnedPoint;
    private String attendedAt;
    private Integer proofStatus;
}
