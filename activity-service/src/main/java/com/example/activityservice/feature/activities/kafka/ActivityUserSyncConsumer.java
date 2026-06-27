package com.example.activityservice.feature.activities.kafka;

import com.example.activityservice.feature.users.dto.UserSnapshot;
import com.example.activityservice.feature.users.service.LocalUserProjectionService;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.JsonNode;
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
            topics = {
                    KafkaTopics.LEGACY_IDENTITY_USER_CREATED,
                    KafkaTopics.LEGACY_IDENTITY_USER_SNAPSHOT,
                    KafkaTopics.USER_SNAPSHOT,
                    KafkaTopics.USER_CREATED,
                    KafkaTopics.USER_UPDATED,
                    KafkaTopics.USER_DEACTIVATED,
                    KafkaTopics.USER_DELETED,
                    KafkaTopics.PROFILE_UPDATED
            },
            groupId = "activity-group-v1")
    public void consumeUserCreated(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = text(root, "eventType");
            if (KafkaEventTypes.USER_DEACTIVATED.equals(eventType) || KafkaEventTypes.USER_DELETED.equals(eventType)) {
                Long userId = readUserId(root);
                Integer status = KafkaEventTypes.USER_DELETED.equals(eventType) ? 2 : 0;
                projectionService.markInactive(userId, status);
                log.info("[ACTIVITY] User projection marked inactive. userId={}, status={}, eventType={}",
                        userId, status, eventType);
                return;
            }

            UserSnapshot snapshot = readSnapshot(root);
            projectionService.upsert(snapshot);
            log.info("[ACTIVITY] User projection synchronized. userId={}, username={}",
                    snapshot.resolvedId(), snapshot.getUsername());
        } catch (Exception e) {
            log.error("Cannot synchronize User event into Activity Service: {}", message, e);
            throw new IllegalStateException("User projection synchronization failed", e);
        }
    }

    private UserSnapshot readSnapshot(JsonNode root) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode payload = root.has("payload") ? root.get("payload") : root;
        return objectMapper.treeToValue(payload, UserSnapshot.class);
    }

    private Long readUserId(JsonNode root) {
        JsonNode payload = root.has("payload") ? root.get("payload") : root;
        JsonNode value = payload.get("userId");
        return value == null || value.isNull() ? null : value.asLong();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
