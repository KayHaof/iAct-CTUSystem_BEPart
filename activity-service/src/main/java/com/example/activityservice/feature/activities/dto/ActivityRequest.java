package com.example.activityservice.feature.activities.dto;

import com.example.activityservice.feature.activitySchedule.dto.ActivityScheduleDto;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityRequest {
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

    private Long semesterId;
    private Long organizerId;

    private Integer status;

    private List<BenefitResponse> benefits;

    private List<ActivityScheduleDto> schedules;
}