package com.example.feature.activities.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityStatsResponse {
    private long activeEvents;      // Số hoạt động đang diễn ra (Đã duyệt)
    private long pendingApprovals;  // Số hoạt động chờ duyệt
    private long totalRegistered;   // Tổng số sinh viên đã đăng ký
    private long pointsDisbursed;   // Tổng điểm đã cấp
}