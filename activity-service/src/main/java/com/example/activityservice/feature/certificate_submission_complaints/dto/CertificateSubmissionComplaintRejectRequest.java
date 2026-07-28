package com.example.activityservice.feature.certificate_submission_complaints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CertificateSubmissionComplaintRejectRequest {
    @NotBlank
    @Size(min = 10, max = 2000)
    private String rejectionReason;
}
