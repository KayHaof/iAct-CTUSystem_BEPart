package com.example.feature.kafka;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCreateListener {

    private final NotificationDispatchService notificationDispatchService;

    @KafkaListener(topics = "notification-create-topic", groupId = "notification-create-group-v1")
    public void handleNotificationCreate(NotificationRequest request) {
        try {
            notificationDispatchService.createAndDispatch(request);
        } catch (Exception e) {
            log.error("Failed to handle notification-create-topic message: {}", e.getMessage());
        }
    }
}

