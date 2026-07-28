package com.example.activityservice.feature.certificate_submissions.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CertificateSubmissionReviewRequest {
    @NotNull(message = "Vui lòng chọn tiêu chí điểm rèn luyện.")
    private Long approvedCategoryId;

    @NotNull(message = "Vui lòng nhập điểm được duyệt.")
    @Min(value = 0, message = "Điểm được duyệt phải lớn hơn hoặc bằng 0.")
    private Integer approvedPoint;

    private String reviewNote;
}
