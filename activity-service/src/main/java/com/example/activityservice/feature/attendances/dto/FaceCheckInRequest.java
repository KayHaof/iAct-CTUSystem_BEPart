package com.example.activityservice.feature.attendances.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FaceCheckInRequest {
    @NotNull(message = "Thieu ma hoat dong")
    private Long activityId;

    private BigDecimal latitude;
    private BigDecimal longitude;
}
