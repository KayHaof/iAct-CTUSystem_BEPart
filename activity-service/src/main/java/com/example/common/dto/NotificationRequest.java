package com.example.common.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String userId;
    private String content;
}
