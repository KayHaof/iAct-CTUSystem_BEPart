package com.example.activityservice.feature.registration.kafka;

import com.example.activityservice.feature.kafka.KafkaEnvelopePublisher;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RegistrationEventProducer extends KafkaEnvelopePublisher {

    public RegistrationEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper, "activity-service");
    }

    public void publishRegistrationCreated(Long userId, Long activityId, String activityTitle) {
        publish(KafkaTopics.REGISTRATION_CREATED, KafkaEventTypes.REGISTRATION_CREATED, "registration",
                aggregateId(userId, activityId), payload(userId, activityId, activityTitle, null));
    }

    public void publishRegistrationCancelled(Long userId, Long activityId, String activityTitle, String reason) {
        publish(KafkaTopics.REGISTRATION_CANCELLED, KafkaEventTypes.REGISTRATION_CANCELLED, "registration",
                aggregateId(userId, activityId), payload(userId, activityId, activityTitle, reason));
    }

    public void publishAttendanceCheckedIn(Long userId, Long activityId, String activityTitle, String sessionTitle) {
        Map<String, Object> payload = payload(userId, activityId, activityTitle, null);
        payload.put("sessionTitle", sessionTitle);
        publish(KafkaTopics.ATTENDANCE_CHECKED_IN, KafkaEventTypes.ATTENDANCE_CHECKED_IN, "attendance",
                aggregateId(userId, activityId), payload);
    }

    private Map<String, Object> payload(Long userId, Long activityId, String activityTitle, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("activityId", activityId);
        payload.put("activityTitle", activityTitle);
        payload.put("reason", reason);
        return payload;
    }

    private String aggregateId(Long userId, Long activityId) {
        return userId + ":" + activityId;
    }
}

