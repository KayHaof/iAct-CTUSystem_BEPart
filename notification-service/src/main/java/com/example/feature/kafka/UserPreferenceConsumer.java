package com.example.feature.kafka;

import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.feature.model.NotificationPreference;
import com.example.feature.repository.NotificationPreferenceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceConsumer {

    private final NotificationPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    KafkaTopics.PREFERENCE_CREATED,
                    KafkaTopics.PREFERENCE_UPDATED,
                    KafkaTopics.PREFERENCE_DELETED
            },
            groupId = "notification-user-preference-v1")
    @Transactional
    public void handlePreferenceEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = text(root, "eventType");
            JsonNode payload = payload(root);
            Long userId = requiredLong(payload, "userId");

            if (KafkaEventTypes.PREFERENCE_DELETED.equals(eventType)) {
                preferenceRepository.deleteByUserId(userId);
                log.info("Notification preferences deleted. userId={}, topic={}", userId, topic);
                return;
            }

            JsonNode settings = payload.get("notificationSettings");
            if (settings == null || settings.isNull()) {
                log.info("Preference event has no notificationSettings. userId={}, topic={}", userId, topic);
                return;
            }

            Boolean newActivityAlert = optionalBoolean(settings, "newActivityAlert");
            Boolean reminderAlert = optionalBoolean(settings, "reminderAlert");

            if (newActivityAlert != null) {
                upsert(userId, 1, newActivityAlert);
                upsert(userId, 2, newActivityAlert);
            }
            if (reminderAlert != null) {
                upsert(userId, 3, reminderAlert);
            }

            log.info("Notification preferences synchronized. userId={}, topic={}", userId, topic);
        } catch (Exception e) {
            log.error("Failed to synchronize notification preferences. topic={}, error={}",
                    topic, e.getMessage(), e);
            throw new IllegalStateException("Notification preference synchronization failed", e);
        }
    }

    private void upsert(Long userId, Integer type, Boolean isEnabled) {
        NotificationPreference preference = preferenceRepository.findByUserIdAndType(userId, type)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(userId)
                        .type(type)
                        .build());
        preference.setIsEnabled(isEnabled);
        preferenceRepository.save(preference);
    }

    private JsonNode payload(JsonNode root) {
        return root.has("payload") ? root.get("payload") : root;
    }

    private Long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.asLong();
    }

    private Boolean optionalBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
