package com.example.feature.kafka;

import com.example.event.ActivityDeletedEvent;
import com.example.event.kafka.KafkaTopics;
import com.example.feature.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityEventListener {
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = KafkaTopics.LEGACY_ACTIVITY_DELETED, groupId = "notification-group")
    @Transactional
    public void handleActivityDeletedEvent(ActivityDeletedEvent event) {
        log.info("Legacy activity cleanup received. activityId={}", event.getActivityId());

        try {
            notificationRepository.deleteByActivityId(event.getActivityId());
            log.info("Legacy activity cleanup completed. activityId={}", event.getActivityId());
        } catch (Exception e) {
            log.error("Legacy activity cleanup failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Legacy activity cleanup failed", e);
        }
    }
}

