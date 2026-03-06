package com.example.feature.activities.dto;

import com.example.common.dto.BenefitDto;
import com.example.common.dto.UserDto;
import com.example.feature.organizers.dto.OrganizerResponse;
import com.example.feature.organizers.model.Organizers;
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

    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Long semesterId;
    private OrganizerResponse organizer;

    private String qrCodeToken;
    private Integer status;

    private Integer RegisteredCount;
    private UserDto createdBy;

    private List<BenefitDto> benefits;
}