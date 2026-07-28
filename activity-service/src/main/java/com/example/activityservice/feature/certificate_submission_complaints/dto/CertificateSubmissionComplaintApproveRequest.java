package com.example.activityservice.feature.certificate_submission_complaints.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CertificateSubmissionComplaintApproveRequest {
    @NotNull
    private Long approvedCategoryId;

    @NotNull
    private Integer approvedPoint;

    @Size(max = 2000)
    private String reviewNote;
}
