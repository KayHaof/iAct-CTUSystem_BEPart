package com.example.activityservice.feature.activities.kafka;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.kafka.KafkaEnvelopePublisher;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ActivityEventProducer extends KafkaEnvelopePublisher {

    public ActivityEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper, "activity-service");
    }

    public void publishUpdated(Activities activity) {
        publish(KafkaTopics.ACTIVITY_UPDATED, KafkaEventTypes.ACTIVITY_UPDATED, "activity",
                String.valueOf(activity.getId()), activityPayload(activity));
    }

    public void publishCreated(Activities activity) {
        publish(KafkaTopics.ACTIVITY_CREATED, KafkaEventTypes.ACTIVITY_CREATED, "activity",
                String.valueOf(activity.getId()), activityPayload(activity));
    }

    public void publishSubmitted(Activities activity) {
        publishSubmitted(activity, List.of());
    }

    public void publishSubmitted(Activities activity, List<Long> recipientIds) {
        publish(KafkaTopics.ACTIVITY_SUBMITTED, KafkaEventTypes.ACTIVITY_SUBMITTED, "activity",
                String.valueOf(activity.getId()), activityPayload(activity, recipientIds));
    }

    public void publishDeleted(Long activityId) {
        publish(KafkaTopics.ACTIVITY_DELETED, KafkaEventTypes.ACTIVITY_DELETED, "activity",
                String.valueOf(activityId), Map.of("activityId", activityId));
    }

    public void publishApproved(Activities activity) {
        publish(KafkaTopics.ACTIVITY_APPROVED, KafkaEventTypes.ACTIVITY_APPROVED, "activity",
                String.valueOf(activity.getId()), activityPayload(activity));
    }

    public void publishRejected(Activities activity) {
        publish(KafkaTopics.ACTIVITY_REJECTED, KafkaEventTypes.ACTIVITY_REJECTED, "activity",
                String.valueOf(activity.getId()), activityPayload(activity));
    }

    public void publishCancelled(Activities activity) {
        publish(KafkaTopics.ACTIVITY_CANCELLED, KafkaEventTypes.ACTIVITY_CANCELLED, "activity",
                String.valueOf(activity.getId()), activityPayload(activity));
    }

    public void publishDraftExpired(Long activityId) {
        publish(KafkaTopics.ACTIVITY_DRAFT_EXPIRED, KafkaEventTypes.ACTIVITY_DRAFT_EXPIRED, "activity",
                String.valueOf(activityId), Map.of("activityId", activityId));
    }

    private Map<String, Object> activityPayload(Activities activity) {
        return activityPayload(activity, List.of());
    }

    private Map<String, Object> activityPayload(Activities activity, List<Long> recipientIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityId", activity.getId());
        payload.put("title", activity.getTitle());
        payload.put("status", activity.getStatus());
        payload.put("departmentId", activity.getDepartmentId());
        payload.put("isFaculty", activity.getIsFaculty());
        payload.put("isExternal", activity.getIsExternal());
        payload.put("registrationStart", activity.getRegistrationStart() != null ? activity.getRegistrationStart().toString() : null);
        payload.put("registrationEnd", activity.getRegistrationEnd() != null ? activity.getRegistrationEnd().toString() : null);
        payload.put("reason", activity.getReason());
        payload.put("ownerUserId", activity.getCreatedBy() != null ? activity.getCreatedBy().getId() : null);
        if (recipientIds != null && !recipientIds.isEmpty()) {
            payload.put("userIds", recipientIds);
        }
        return payload;
    }
}
