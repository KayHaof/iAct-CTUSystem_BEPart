package com.example.userservice.feature.classes.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepresentativeActivityPermissionResponse {
    private Long id;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long classId;
    private String classCode;
    private String className;
    private Long departmentId;
    private String departmentName;
    private String representativeType;
    private Boolean isActive;
    private boolean canCreateActivity;
}
