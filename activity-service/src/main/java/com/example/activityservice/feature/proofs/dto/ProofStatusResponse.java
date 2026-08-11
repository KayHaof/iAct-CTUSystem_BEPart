package com.example.activityservice.feature.proofs.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProofStatusResponse {
    private Long activityId;
    private Long registrationId;
    private Integer registrationStatus;
    private String attendanceStatus;
    private Integer proofStatus; // 0=not submitted, 1=pending, 2=approved, 3=rejected
    private Boolean submitted;
    private Boolean canSubmit;
    private Boolean canResubmit;
    private Long proofId;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
