package com.example.activityservice.feature.attendances.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponse {
    private Long id;
    private Long registrationId;
    private Long scheduleId;
    private String scheduleTitle;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private String attendanceStatus;
    private Integer status;
    private Integer method;
    private String message;
}
