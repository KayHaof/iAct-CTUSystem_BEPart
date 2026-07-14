package com.example.activityservice.feature.locations.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LocationBookingResponse {
    private Long id;
    private Long activityId;
    private Long locationId;
    private Long scheduleId;
    private String scheduleTitle;
    private String locationName;
    private String locationCode;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String statusLabel;
    private Long requestedBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
