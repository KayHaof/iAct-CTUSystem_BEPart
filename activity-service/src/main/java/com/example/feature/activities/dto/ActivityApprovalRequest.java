package com.example.feature.activities.dto;

import lombok.Data;

@Data
public class ActivityApprovalRequest {
    private Integer status;

    private String rejectReason;

    private String cancelReason;
}