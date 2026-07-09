package com.example.activityservice.feature.departments.dto;

import lombok.Data;

@Data
public class DepartmentLookupResponse {
    private Long id;
    private String name;
    private String code;
    private Boolean isActive;
}
