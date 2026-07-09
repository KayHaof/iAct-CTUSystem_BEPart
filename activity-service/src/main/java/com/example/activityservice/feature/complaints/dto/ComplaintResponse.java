package com.example.activityservice.feature.complaints.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ComplaintResponse {
    private Long id;
    private Long registrationId;
    private Long activityId;
    private String activityTitle;
    private String detail;
    private String evidenceUrl;
    private String response;
    private Integer status;
    private String statusLabel;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
