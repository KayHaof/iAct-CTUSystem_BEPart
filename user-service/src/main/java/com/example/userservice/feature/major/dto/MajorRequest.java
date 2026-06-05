package com.example.userservice.feature.major.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MajorRequest {
    @NotBlank(message = "Major name is required")
    private String name;

    private String code;

    private String programType;

    @NotNull(message = "Department id is required")
    private Long departmentId;

    private Boolean isActive;
}
