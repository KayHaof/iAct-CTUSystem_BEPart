package com.example.activityservice.feature.registration.dto;

import lombok.Data;

@Data
public class RegistrationStatusUpdateRequest {
    private Integer status;
    private Boolean processViolation;
}
