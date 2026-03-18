package com.example.feature.major.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MajorResponse {
    private Long id;
    private String name;
    private String programType;
    private Long departmentId;
    private String departmentName;
}
