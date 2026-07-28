package com.example.activityservice.feature.certificate_submissions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificateSubmissionRejectRequest {
    @NotBlank(message = "Vui lòng nhập lý do từ chối.")
    private String reason;
}
