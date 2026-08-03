package com.example.activityservice.feature.complaints.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ComplaintEligibleActivityResponse {
    private Long registrationId;
    private Long activityId;
    private String activityTitle;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private Integer faceAttemptCount;
    private Boolean faceAttemptExhausted;
    private String eligibilityReason;
    private ComplaintResponse complaint;
}
