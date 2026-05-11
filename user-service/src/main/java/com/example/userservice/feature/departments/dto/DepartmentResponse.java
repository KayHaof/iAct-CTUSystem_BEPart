package com.example.userservice.feature.departments.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private String description;
}