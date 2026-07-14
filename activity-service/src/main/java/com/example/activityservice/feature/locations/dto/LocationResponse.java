package com.example.activityservice.feature.locations.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LocationResponse {
    private Long id;
    private String name;
    private String code;
    private String type;
    private String description;
    private String address;
    private String building;
    private String floor;
    private String room;
    private Integer capacity;
    private Long managerDepartmentId;
    private Long managerUserId;
    private String contactName;
    private String contactPhone;
    private Boolean adminManaged;
    private Boolean isBookable;
    private String availabilityStatus;
    private Boolean isActive;
    private String unavailableReason;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
