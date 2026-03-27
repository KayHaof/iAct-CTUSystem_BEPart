package com.example.feature.activities.dto;

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
}