package com.example.activityservice.feature.kafka;

import com.example.event.kafka.KafkaEventEnvelope;
import com.example.event.kafka.KafkaEventMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class KafkaEnvelopePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String producerName;

    protected void publish(String topic, String eventType, String aggregateType, String aggregateId, Object payload) {
        String eventId = UUID.randomUUID().toString();
        KafkaEventEnvelope<Object> envelope = KafkaEventEnvelope.builder()
                .eventId(eventId)
                .eventVersion(1)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .occurredAt(Instant.now().toString())
                .producer(producerName)
                .payload(payload)
                .metadata(KafkaEventMetadata.builder().source("service").build())
                .build();

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Kafka event envelope", exception);
        }

        Runnable sendAction = () -> kafkaTemplate.send(topic, aggregateId, jsonPayload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Cannot send Kafka event. topic={}, eventId={}, aggregateId={}",
                                topic, eventId, aggregateId, exception);
                    } else {
                        log.info("Kafka event sent. topic={}, eventId={}, aggregateId={}",
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

