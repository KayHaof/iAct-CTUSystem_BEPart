package com.example.activityservice.feature.proofs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProofActivitySummaryResponse {
    private Long activityId;
    private long totalRegisteredStudents;
    private long totalEligibleStudents;
    private long totalSubmittedProofs;
    private long totalSubmittedStudents;
    private long totalNotSubmittedEligibleStudents;
    private long pendingProofs;
    private long approvedProofs;
    private long rejectedProofs;
    private long absentStudents;
    private long unreviewedAbsentStudents;
}
