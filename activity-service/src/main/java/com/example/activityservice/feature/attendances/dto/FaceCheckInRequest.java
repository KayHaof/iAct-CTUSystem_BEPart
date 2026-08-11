package com.example.activityservice.feature.attendances.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FaceCheckInRequest {
    @NotNull(message = "Thiếu mã hoạt động")
    private Long activityId;

    private Long scheduleId;

    private BigDecimal latitude;
    private BigDecimal longitude;
}
