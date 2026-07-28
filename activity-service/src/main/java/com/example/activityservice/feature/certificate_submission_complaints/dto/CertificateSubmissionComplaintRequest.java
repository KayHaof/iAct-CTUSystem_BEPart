package com.example.activityservice.feature.certificate_submission_complaints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CertificateSubmissionComplaintRequest {
    @NotNull
    private Long submissionId;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String complaintReason;
}
