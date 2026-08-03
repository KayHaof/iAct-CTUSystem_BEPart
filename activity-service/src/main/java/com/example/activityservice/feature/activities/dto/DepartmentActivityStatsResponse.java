package com.example.activityservice.feature.activities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentActivityStatsResponse {
    private Long departmentId;
    private String departmentName;
    private long pendingReview;
    private long approvedThisTerm;
    private long rejected;
    private long total;
}
