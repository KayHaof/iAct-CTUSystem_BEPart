package com.example.feature.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long userId;
    private String content;

    private Long activityId;
    private String title;
    private String message;
    private Integer type;
}
