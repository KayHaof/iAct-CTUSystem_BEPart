package com.example.feature.kafka;

import com.example.event.kafka.KafkaTopics;
import com.example.feature.dto.NotificationRequest;
import com.example.feature.service.NotificationDispatchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandConsumer {

    private final NotificationDispatchService notificationDispatchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_CREATED, groupId = "notification-create-v1")
    public void handleNotificationCreate(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            NotificationRequest request = readRequest(message, topic);
            notificationDispatchService.createAndDispatch(request);
        } catch (Exception e) {
            log.error("Failed to handle standardized notification command: {}", e.getMessage(), e);
            throw new IllegalStateException("Standardized notification command failed", e);
        }
    }

    private NotificationRequest readRequest(String message, String topic) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.has("payload") ? root.get("payload") : root;
        NotificationRequest request = objectMapper.treeToValue(payload, NotificationRequest.class);
        if (root.has("eventId")) {
            request.setSourceEventId(root.get("eventId").asText());
            request.setSourceTopic(topic);
        }
        return request;
    }
}
