package com.example.activityservice.feature.users.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepresentativeActivityPermissionResponse {
    private Long studentId;
    private Long classId;
    private String classCode;
    private String className;
    private Long departmentId;
    private String departmentName;
    private String representativeType;
    private boolean canCreateActivity;
}
