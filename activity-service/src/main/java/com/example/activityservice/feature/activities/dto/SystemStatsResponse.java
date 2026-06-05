package com.example.activityservice.feature.activities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsResponse {
    private Long semesterId;
    private String semesterName;
    
    // User stats
    private Long totalStudents;
    private Long totalDepartments;
    private Long totalAdmins;
    private Long newAccountsThisMonth;
    
    // Activity stats
    private Integer totalActivities;
    private Integer pendingApproval;
    private Integer approvedThisSemester;
    private Integer rejected;
    private Integer cancelled;
    private Double approvalRate;
    
    // Registration stats
    private Long totalRegistrations;
    private Long totalCancellations;
    private Double cancellationRate;
    
    // Attendance stats
    private Long totalAttendances;
    private Double averageAttendanceRate;
    
    // Points stats
    private Double averagePointsPerStudent;
    private Long studentsWithGoodScore;  // > 80
    
    // Category breakdown
    private CategoryDistribution[] categoryDistribution;
    
    // Monthly trend
    private MonthlyTrend[] monthlyTrend;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CategoryDistribution {
    private String categoryName;
    private Integer activityCount;
    private Long participantCount;
    private Double percentage;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MonthlyTrend {
    private Integer month;
    private Integer year;
    private Integer newActivities;
    private Integer newRegistrations;
    private Double attendanceRate;
}
