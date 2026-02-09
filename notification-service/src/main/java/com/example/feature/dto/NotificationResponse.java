package com.example.feature.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;

    private String title;
    private String message;
    private Integer type;
    private Boolean isRead;
    private LocalDateTime createdAt;

    private Long activityId;
    private String activityTitle;
    private String activityThumbnail;
}