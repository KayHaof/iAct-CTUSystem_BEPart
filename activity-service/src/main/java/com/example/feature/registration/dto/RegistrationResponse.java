package com.example.feature.registration.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegistrationResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private Long activityId;
    private String activityTitle;
    private LocalDateTime registeredAt;
    private Integer status; // 0=registered, 1=attended, 2=cancelled
    private String cancelReason;

    private LocalDateTime attendedAt;
    private Boolean isAttended;
}
