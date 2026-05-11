package com.example.activityservice.feature.activities.dto;

import com.example.activityservice.common.dto.UserDto;
import com.example.activityservice.feature.activitySchedule.dto.ActivityScheduleDto;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.organizers.dto.OrganizerResponse;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityResponse {
    private Long id;

    private String title;
    private String description;
    private String content;

    private String location;
    private Integer maxParticipants;
    private String coverImage;
    private String thumbnail;
    private String sourceLink;
    private Boolean isExternal;
    private Boolean isFaculty;

    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    private Long semesterId;
    private OrganizerResponse organizer;

    private Integer departmentId;
    private String departmentName;

    private String qrCodeToken;
    private Integer status;

    private Integer registeredCount;
    private UserDto createdBy;

    private String reason;
    private UserDto handledBy;
    private LocalDateTime handledAt;

    private List<BenefitResponse> benefits;

    private List<ActivityScheduleDto> schedules;
}