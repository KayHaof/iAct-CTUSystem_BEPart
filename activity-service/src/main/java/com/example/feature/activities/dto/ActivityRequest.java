package com.example.feature.activities.dto;

import com.example.common.dto.BenefitDto;
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

    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Long semesterId;
    private Long organizerId;

    private Integer status;

    private List<BenefitDto> benefits;
}