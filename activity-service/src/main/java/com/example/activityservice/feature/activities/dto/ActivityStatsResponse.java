package com.example.activityservice.feature.activities.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatsResponse {
    private long pendingReview;    // Chờ duyệt
    private long approvedThisTerm; // Đã duyệt
    private long rejected;         // Đã từ chối
    @Builder.Default
    private List<DepartmentActivityStatsResponse> byDepartment = List.of();
}
