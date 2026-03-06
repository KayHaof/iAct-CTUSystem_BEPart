package com.example.feature.registration.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class RegistrationRequest {
    @NotNull(message = "Activity ID không được để trống")
    private Long activityId;
    private String cancelReason;
}
