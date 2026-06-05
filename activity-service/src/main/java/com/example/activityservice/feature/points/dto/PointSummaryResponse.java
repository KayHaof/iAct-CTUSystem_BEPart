package com.example.activityservice.feature.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointSummaryResponse {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long semesterId;
    private String semesterName;
    private Integer totalPoint;
    private Integer maxPoint;
    private Double percentage;
    private String status; // "excellent", "good", "warning", "danger"
    private List<CategoryPointItem> categoryBreakdown;
    private List<String> warnings;
}
