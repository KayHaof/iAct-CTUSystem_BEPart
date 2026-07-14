package com.example.activityservice.feature.locations.dto;

import lombok.Data;

@Data
public class LocationAvailabilityRequest {
    private Boolean isBookable;
    private String availabilityStatus;
    private String unavailableReason;
}
