package com.example.feature.kafka;

import com.example.event.kafka.KafkaEventEnvelope;
import com.example.event.kafka.KafkaEventMetadata;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.feature.dto.NotificationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationLifecycleProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishDispatched(NotificationResponse response) {
        publish(KafkaTopics.NOTIFICATION_DISPATCHED, KafkaEventTypes.NOTIFICATION_DISPATCHED,
                String.valueOf(response.getId()), response, "dispatch");
    }

    public void publishRead(Long notificationId, Long userId, String readAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", notificationId);
        payload.put("userId", userId);
        payload.put("readAt", readAt);
        publish(KafkaTopics.NOTIFICATION_READ, KafkaEventTypes.NOTIFICATION_READ,
                String.valueOf(notificationId), payload, "read");
    }

    public void publishDeleted(Long notificationId, Long userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", notificationId);
        payload.put("userId", userId);
        publish(KafkaTopics.NOTIFICATION_DELETED, KafkaEventTypes.NOTIFICATION_DELETED,
                String.valueOf(notificationId), payload, "delete");
    }

    public void publishDeliveryFailed(Long notificationId, Long userId, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", notificationId);
        payload.put("userId", userId);
        payload.put("reason", reason);
        publish(KafkaTopics.NOTIFICATION_DELIVERY_FAILED, KafkaEventTypes.NOTIFICATION_DELIVERY_FAILED,
                String.valueOf(notificationId), payload, "delivery");
    }

    private void publish(String topic, String eventType, String aggregateId, Object payloadBody, String source) {
        String eventId = UUID.randomUUID().toString();
        KafkaEventEnvelope<Object> envelope = KafkaEventEnvelope.builder()
                .eventId(eventId)
                .eventVersion(1)
                .eventType(eventType)
                .aggregateType("notification")
                .aggregateId(aggregateId)
                .occurredAt(Instant.now().toString())
                .producer("notification-service")
                .payload(payloadBody)
                .metadata(KafkaEventMetadata.builder().source(source).build())
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize notification lifecycle event", exception);
        }

        Runnable sendAction = () -> kafkaTemplate.send(topic, aggregateId, payload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Cannot send notification lifecycle event. topic={}, eventId={}, notificationId={}",
                                topic, eventId, aggregateId, exception);
                    } else {
                        log.info("Notification lifecycle event sent. topic={}, eventId={}, notificationId={}",
                                topic, eventId, aggregateId);
                    }
                });

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAction.run();
                }
            });
        } else {
            sendAction.run();
        }
    }
}
