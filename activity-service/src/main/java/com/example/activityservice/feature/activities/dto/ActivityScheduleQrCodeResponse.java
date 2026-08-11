package com.example.activityservice.feature.activities.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityScheduleQrCodeResponse {
    private Long activityId;
    private String activityTitle;
    private Long scheduleId;
    private String scheduleTitle;
    private LocalDateTime scheduleStartTime;
    private LocalDateTime scheduleEndTime;
    private String location;
    private String qrCodeImage;
}
