package com.example.userservice.feature.user_profile.kafka;

import com.example.util.UtcDateTime;
import com.example.event.StudentFaceEmbeddingEvent;
import com.example.event.kafka.KafkaEventEnvelope;
import com.example.event.kafka.KafkaEventMetadata;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.userservice.feature.user_profile.model.StudentFaceEmbedding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentFaceEmbeddingEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUpserted(StudentFaceEmbedding embedding) {
        publish(embedding, KafkaEventTypes.STUDENT_FACE_EMBEDDING_UPSERTED);
    }

    public void publishRevoked(StudentFaceEmbedding embedding) {
        publish(embedding, KafkaEventTypes.STUDENT_FACE_EMBEDDING_REVOKED);
    }

    private void publish(StudentFaceEmbedding embedding, String eventType) {
        String eventId = UUID.randomUUID().toString();
        String aggregateId = String.valueOf(embedding.getUserId());
        Runnable sendAction = () -> send(embedding, eventType, eventId, aggregateId);

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

    private void send(StudentFaceEmbedding embedding, String eventType, String eventId, String aggregateId) {
        StudentFaceEmbeddingEvent payload = toPayload(embedding);
        KafkaEventEnvelope<StudentFaceEmbeddingEvent> envelope = KafkaEventEnvelope.<StudentFaceEmbeddingEvent>builder()
                .eventId(eventId)
                .eventVersion(1)
                .eventType(eventType)
                .aggregateType("student-face-embedding")
                .aggregateId(aggregateId)
                .occurredAt(Instant.now().toString())
                .producer("user-service")
                .payload(payload)
                .metadata(KafkaEventMetadata.builder().source("student-face-embedding").build())
                .build();

        String message;
        try {
            message = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize student face embedding event", exception);
        }

        kafkaTemplate
                .send(KafkaTopics.STUDENT_FACE_EMBEDDING_SNAPSHOT, aggregateId, message)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Cannot send student face embedding event. eventId={}, userId={}",
                                eventId, embedding.getUserId(), exception);
                    } else {
                        log.info("Student face embedding event sent. eventId={}, userId={}, eventType={}",
                                eventId, embedding.getUserId(), eventType);
                    }
                });
    }

    private StudentFaceEmbeddingEvent toPayload(StudentFaceEmbedding embedding) {
        StudentFaceEmbeddingEvent payload = new StudentFaceEmbeddingEvent();
        payload.setUserId(embedding.getUserId());
        payload.setReferenceImageUrl(embedding.getReferenceImageUrl());
        payload.setReferenceImagePublicId(embedding.getReferenceImagePublicId());
        payload.setEmbeddingVector(embedding.getEmbeddingVector());
        payload.setVectorSize(embedding.getVectorSize());
        payload.setModelName(embedding.getModelName());
        payload.setDetectorBackend(embedding.getDetectorBackend());
        payload.setNormalizationMethod(embedding.getNormalizationMethod());
        payload.setDistanceMetric(embedding.getDistanceMetric());
        payload.setQualityScore(embedding.getQualityScore());
        payload.setFaceConfidence(embedding.getFaceConfidence());
        payload.setEmbeddingVersion(embedding.getEmbeddingVersion());
        payload.setStatus(embedding.getStatus());
        payload.setLastVerifiedAt(format(embedding.getLastVerifiedAt()));
        payload.setCreatedAt(format(embedding.getCreatedAt()));
        payload.setUpdatedAt(format(embedding.getUpdatedAt()));
        payload.setRevokedAt(format(embedding.getRevokedAt()));
        payload.setRevokedReason(embedding.getRevokedReason());
        return payload;
    }

    private String format(LocalDateTime value) {
        return UtcDateTime.format(value);
    }
}
