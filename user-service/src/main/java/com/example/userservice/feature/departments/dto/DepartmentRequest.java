package com.example.userservice.feature.departments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentRequest {
    @NotBlank(message = "Department name is required")
    private String name;

    private String code;

    private String description;

    private String phone;

    private String address;

    private String avatarUrl;

    private Boolean isActive;
}
