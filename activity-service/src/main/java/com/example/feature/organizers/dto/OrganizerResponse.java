package com.example.feature.organizers.dto;

import lombok.Data;

@Data
public class OrganizerResponse {
    private Long id;
    private String name;
    private Long departmentId;
    private Long representativeId;
}
