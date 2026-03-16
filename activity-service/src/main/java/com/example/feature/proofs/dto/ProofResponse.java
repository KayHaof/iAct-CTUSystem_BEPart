package com.example.feature.proofs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProofResponse {
    private Long id;
    private Long activityId;
    private String imageUrl;
    private String description;
    private Integer status; // 0: Pending, 1: Approved, 2: Rejected
    private String rejectionReason;
}
