package com.example.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String userId;
    private String content;
}
