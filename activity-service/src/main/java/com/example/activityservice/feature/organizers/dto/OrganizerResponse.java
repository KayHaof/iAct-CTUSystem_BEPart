package com.example.activityservice.feature.organizers.dto;

import lombok.Data;

@Data
public class OrganizerResponse {
    private Long id;
    private String fullName;
    private Long departmentId;
    private String departmentName;
    private Long representativeId;
}
