package com.example.activityservice.common.dto;

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
