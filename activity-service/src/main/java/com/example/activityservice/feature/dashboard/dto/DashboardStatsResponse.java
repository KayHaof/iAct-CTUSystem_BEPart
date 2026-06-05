package com.example.activityservice.feature.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Integer totalActivities;
    private Integer activeActivities;
    private Integer pendingActivities;
    private Integer totalStudents;
    private Integer totalDepartments;
    private Integer totalMajors;
    private List<RecentActivityDto> recentActivities;
}
