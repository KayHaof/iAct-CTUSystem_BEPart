package com.example.feature.kafka;

import com.example.event.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationLifecycleConsumer {

    @KafkaListener(
            topics = {
                    KafkaTopics.NOTIFICATION_DISPATCHED,
                    KafkaTopics.NOTIFICATION_READ,
                    KafkaTopics.NOTIFICATION_DELETED,
                    KafkaTopics.NOTIFICATION_DELIVERY_FAILED
            },
            groupId = "notification-lifecycle-audit-v1")
    public void auditLifecycleEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Notification lifecycle event received. topic={}, payload={}", topic, message);
    }
}
