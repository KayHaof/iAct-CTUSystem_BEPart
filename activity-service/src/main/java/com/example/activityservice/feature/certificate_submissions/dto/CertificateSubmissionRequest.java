package com.example.activityservice.feature.certificate_submissions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificateSubmissionRequest {
    @NotBlank(message = "Vui lòng cung cấp link ảnh giấy khen.")
    private String imageUrl;

    private Long semesterId;

    private String studentNote;
}
