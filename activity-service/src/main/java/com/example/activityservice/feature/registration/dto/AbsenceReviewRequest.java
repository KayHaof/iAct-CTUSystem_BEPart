package com.example.activityservice.feature.registration.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AbsenceReviewRequest {
    @Size(max = 2000, message = "Ghi chú xử lý vắng mặt không được vượt quá 2000 ký tự")
    private String note;
}
