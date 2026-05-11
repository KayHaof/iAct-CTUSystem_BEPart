package com.example.activityservice.feature.activitySchedule.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityScheduleDto {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
}
