package com.example.activityservice.feature.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {
    private Long id;
    private String title;
    private String departmentName;
    private String startDate;
    private Integer status;
    private Long registeredCount;
    private Integer maxParticipants;
    private String thumbnail;
}
