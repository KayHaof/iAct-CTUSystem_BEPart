package com.example.userservice.feature.major.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MajorResponse {
    private Long id;
    private String name;
    private String code;
    private String programType;
    private Long departmentId;
    private String departmentName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
