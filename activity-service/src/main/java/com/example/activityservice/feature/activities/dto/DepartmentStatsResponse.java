package com.example.activityservice.feature.activities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentStatsResponse {
    private Long departmentId;
    private String departmentName;
    private Long semesterId;
    private String semesterName;
    
    // Activity counts
    private Integer totalActivities;
    private Integer pendingActivities;
    private Integer approvedActivities;
    private Integer rejectedActivities;
    private Integer cancelledActivities;
    
    // Participation stats
    private Integer totalRegistrations;
    private Integer totalAttendances;
    private Integer totalCancellations;
    private Double attendanceRate;
    
    // Points stats
    private Integer totalPointsAwarded;
    private Integer uniqueStudentsParticipated;
    
    // Monthly breakdown
    private MonthlyStats[] monthlyStats;
    
    // Category breakdown
    private CategoryStats[] categoryStats;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MonthlyStats {
    private Integer month;
    private Integer year;
    private Integer activityCount;
    private Integer registrationCount;
    private Integer attendanceCount;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CategoryStats {
    private String categoryName;
    private Integer activityCount;
    private Integer participantCount;
    private Double percentage;
}
