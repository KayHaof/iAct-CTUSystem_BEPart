package com.example.activityservice.feature.activities.kafka;

import com.example.activityservice.feature.users.dto.UserSnapshot;
import com.example.activityservice.feature.users.service.LocalUserProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityUserSyncConsumer {

    private final LocalUserProjectionService projectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"iact.identity.user.created", "iact.identity.user.snapshot"},
            groupId = "activity-group-v1")
    public void consumeUserCreated(String message) {
        try {
            UserSnapshot snapshot = objectMapper.readValue(message, UserSnapshot.class);
            projectionService.upsert(snapshot);
            log.info("[ACTIVITY] Đã đồng bộ User ID: {}, username: {}",
                    snapshot.resolvedId(), snapshot.getUsername());
        } catch (Exception e) {
            log.error("Không thể đồng bộ User event vào Activity Service: {}", message, e);
            throw new IllegalStateException("User projection synchronization failed", e);
        }
    }
}
