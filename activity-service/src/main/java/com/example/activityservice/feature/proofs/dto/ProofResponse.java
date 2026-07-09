package com.example.activityservice.feature.proofs.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProofResponse {
    private Long id;
    private Long registrationId;
    private Long activityId;
    private String activityTitle;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String studentAvatarUrl;
    private String imageUrl;
    private String description;
    private Integer status; // 0: Pending, 1: Approved, 2: Rejected
    private String rejectionReason;
    private Long verifiedBy;
    private LocalDateTime verifiedTime;
    private LocalDateTime submittedAt;
}
