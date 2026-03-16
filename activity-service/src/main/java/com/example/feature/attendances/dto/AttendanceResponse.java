package com.example.feature.attendances.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponse {
    private Long id;
    private Long registrationId;
    private LocalDateTime checkinTime;
    private Integer method;
    private String message;
}