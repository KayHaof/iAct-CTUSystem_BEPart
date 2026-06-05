package com.example.activityservice.feature.registration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "iact.notification.created";

    @Async
    public void sendRegistrationSuccess(Long userId, Long activityId, String activityTitle, String message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("activityId", activityId);
            payload.put("title", "Dang ky thanh cong");
            payload.put("content", message != null ? message : "Ban da dang ky thanh cong hoat dong: " + activityTitle);
            payload.put("type", 1);
            payload.put("message", "Dang ky thanh cong hoat dong: " + activityTitle);

            kafkaTemplate.send(TOPIC, payload);
            log.info("Kafka: Registration success notification sent for userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send registration success notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendCancellationSuccess(Long userId, Long activityId, String activityTitle, String reason) {
        try {
            String content = reason != null && !reason.isBlank()
                    ? "Ban da huy dang ky hoat dong: " + activityTitle + ". Ly do: " + reason
                    : "Ban da huy dang ky hoat dong: " + activityTitle;

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("activityId", activityId);
            payload.put("title", "Huy dang ky thanh cong");
            payload.put("content", content);
            payload.put("type", 2);
            payload.put("message", content);

            kafkaTemplate.send(TOPIC, payload);
            log.info("Kafka: Cancellation notification sent for userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendCheckInSuccess(Long userId, Long activityId, String activityTitle, String sessionTitle) {
        try {
            String content = "Ban da check-in thanh cong buoi: " + sessionTitle + " cua hoat dong: " + activityTitle;

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("activityId", activityId);
            payload.put("title", "Check-in thanh cong");
            payload.put("content", content);
            payload.put("type", 3);
            payload.put("message", content);

            kafkaTemplate.send(TOPIC, payload);
            log.info("Kafka: Check-in notification sent for userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send check-in notification: {}", e.getMessage());
        }
    }
}
