package com.example.feature.kafka;

import com.example.event.kafka.KafkaTopics;
import com.example.feature.dto.NotificationRequest;
import com.example.feature.repository.NotificationRepository;
import com.example.feature.service.NotificationDispatchService;
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
public class NotificationCommandRequestConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_CLEANUP_REQUESTED,
            groupId = "notification-cleanup-command-v1")
    @Transactional
    public void handleCleanupRequested(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = payload(root);
            Long activityId = optionalLong(payload, "activityId");
            if (activityId == null) {
                log.info("Cleanup command ignored because activityId is missing. topic={}", topic);
                return;
            }
            notificationRepository.deleteByActivityId(activityId);
            log.info("Notification cleanup command handled. activityId={}, eventId={}",
                    activityId, text(root, "eventId"));
        } catch (Exception e) {
            log.error("Failed to handle notification cleanup command. topic={}, error={}",
                    topic, e.getMessage(), e);
            throw new IllegalStateException("Notification cleanup command failed", e);
        }
    }

    @KafkaListener(
            topics = {
                    KafkaTopics.NOTIFICATION_BROADCAST_REQUESTED,
                    KafkaTopics.NOTIFICATION_URGENT_REQUESTED
            },
            groupId = "notification-broadcast-command-v1")
    @Transactional
    public void handleBroadcastRequested(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = payload(root);
            JsonNode userIds = payload.get("userIds");

            if (userIds != null && userIds.isArray() && !userIds.isEmpty()) {
                for (JsonNode userId : userIds) {
                    dispatchService.createAndDispatch(toRequest(root, payload, topic, userId.asLong()));
                }
            } else {
                dispatchService.createAndDispatch(toRequest(root, payload, topic, null));
            }

            log.info("Notification command handled. topic={}, eventId={}", topic, text(root, "eventId"));
        } catch (Exception e) {
            log.error("Failed to handle notification command. topic={}, error={}",
                    topic, e.getMessage(), e);
            throw new IllegalStateException("Notification command failed", e);
        }
    }

    private NotificationRequest toRequest(JsonNode root, JsonNode payload, String topic, Long userId) {
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setTitle(defaultText(payload, "title", "Thong bao he thong"));
        request.setMessage(defaultText(payload, "message", defaultText(payload, "content", "")));
        request.setContent(request.getMessage());
        request.setType(resolveType(payload, topic));
        request.setActivityId(optionalLong(payload, "activityId"));
        request.setReferenceType(defaultText(payload, "referenceType",
                KafkaTopics.NOTIFICATION_URGENT_REQUESTED.equals(topic) ? "urgent" : "broadcast"));
        request.setSourceTopic(topic);
        String eventId = text(root, "eventId");
        request.setSourceEventId(userId == null || eventId == null ? eventId : eventId + ":" + userId);
        return request;
    }

    private Integer resolveType(JsonNode payload, String topic) {
        Integer type = optionalInt(payload, "type");
        if (type != null) {
            return type;
        }
        Integer priority = optionalInt(payload, "priority");
        if (priority != null) {
            return priority >= 3 ? 3 : priority == 2 ? 2 : 1;
        }
        return KafkaTopics.NOTIFICATION_URGENT_REQUESTED.equals(topic) ? 3 : 2;
    }

    private JsonNode payload(JsonNode root) {
        return root.has("payload") ? root.get("payload") : root;
    }

    private Long optionalLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private Integer optionalInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private String defaultText(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
