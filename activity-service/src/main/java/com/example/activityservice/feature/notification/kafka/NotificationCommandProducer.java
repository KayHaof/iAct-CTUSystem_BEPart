package com.example.activityservice.feature.notification.kafka;

import com.example.activityservice.common.dto.NotificationRequest;
import com.example.activityservice.feature.kafka.KafkaEnvelopePublisher;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationCommandProducer extends KafkaEnvelopePublisher {

    public NotificationCommandProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper, "activity-service");
    }

    public void publishCreated(NotificationRequest request) {
        String aggregateId = request.getActivityId() != null
                ? String.valueOf(request.getActivityId())
                : String.valueOf(request.getUserId());
        publish(KafkaTopics.NOTIFICATION_CREATED, KafkaEventTypes.NOTIFICATION_CREATED, "notification",
                aggregateId, request);
    }
}

