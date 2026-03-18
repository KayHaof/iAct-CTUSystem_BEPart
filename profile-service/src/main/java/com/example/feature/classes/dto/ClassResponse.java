package com.example.feature.classes.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassResponse {
    private Long id;
    private String classCode;
    private String name;
    private Integer academicYear;
    private Long majorId;
    private String majorName;
    private Long departmentId;
    private String departmentName;
}
