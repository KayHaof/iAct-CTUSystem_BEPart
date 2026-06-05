package com.example.activityservice.feature.attendances.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatisticsResponse {
    private Long activityId;
    private Long sessionId;
    private Integer totalRegistrations;
    private Integer totalAttendances;
    private Integer totalAbsences;
    private Double attendanceRate;
    private Integer presentMale;
    private Integer presentFemale;
    private Integer absentMale;
    private Integer absentFemale;
}
