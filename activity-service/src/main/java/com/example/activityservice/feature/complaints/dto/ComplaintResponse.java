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
    private Long semesterId;
    private String semesterName;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String detail;
    private String reason;
    private String evidenceUrl;
    private String response;
    private String detailResponse;
    private Integer status;
    private String statusLabel;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
