package com.example.activityservice.feature.locations.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocationBookingRequest {
    private Long locationId;
    private Long scheduleId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
