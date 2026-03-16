package com.example.feature.proofs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProofSubmissionRequest {
    @NotNull(message = "Thiếu mã hoạt động")
    private Long activityId;

    @NotBlank(message = "Vui lòng tải lên ảnh minh chứng")
    private String imageUrl;

    private String description;
}
