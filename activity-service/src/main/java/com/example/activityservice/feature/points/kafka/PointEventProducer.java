package com.example.activityservice.feature.points.kafka;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.kafka.KafkaEnvelopePublisher;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PointEventProducer extends KafkaEnvelopePublisher {

    public PointEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper, "activity-service");
    }

    public void publishAwarded(Long userId, Activities activity) {
        publish(KafkaTopics.POINT_AWARDED, KafkaEventTypes.POINT_AWARDED, "point",
                aggregateId(userId, activity), payload(userId, activity, "awarded"));
    }

    public void publishCertificateAwarded(Long userId, Long semesterId, String certificateTitle, Integer point) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("activityId", null);
        payload.put("activityTitle", certificateTitle);
        payload.put("certificateTitle", certificateTitle);
        payload.put("semesterId", semesterId);
        payload.put("point", point);
        payload.put("sourceType", "CERTIFICATE_SUBMISSION");
        payload.put("reason", "certificate-submission-approved");
        publish(KafkaTopics.POINT_AWARDED, KafkaEventTypes.POINT_AWARDED, "point",
                userId + ":certificate:" + certificateTitle, payload);
    }

    public void publishRecalculated(Long userId, Activities activity, String reason) {
        Map<String, Object> payload = payload(userId, activity, reason);
        publish(KafkaTopics.POINT_RECALCULATED, KafkaEventTypes.POINT_RECALCULATED, "point",
                aggregateId(userId, activity), payload);
    }

    public void publishRevoked(Long userId, Activities activity, String reason) {
        publish(KafkaTopics.POINT_REVOKED, KafkaEventTypes.POINT_REVOKED, "point",
                aggregateId(userId, activity), payload(userId, activity, reason));
    }

    private Map<String, Object> payload(Long userId, Activities activity, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("activityId", activity != null ? activity.getId() : null);
        payload.put("activityTitle", activity != null ? activity.getTitle() : null);
        payload.put("semesterId", activity != null && activity.getSemester() != null ? activity.getSemester().getId() : null);
        payload.put("reason", reason);
        return payload;
    }

    private String aggregateId(Long userId, Activities activity) {
        Long activityId = activity != null ? activity.getId() : null;
        return userId + ":" + activityId;
    }
}
