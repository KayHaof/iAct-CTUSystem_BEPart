package com.example.activityservice.feature.face_embedding.kafka;

import com.example.activityservice.feature.face_embedding.model.StudentFaceEmbeddingProjection;
import com.example.activityservice.feature.face_embedding.service.StudentFaceEmbeddingProjectionService;
import com.example.event.StudentFaceEmbeddingEvent;
import com.example.event.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentFaceEmbeddingSyncConsumer {

    private final StudentFaceEmbeddingProjectionService projectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.STUDENT_FACE_EMBEDDING_SNAPSHOT,
            groupId = "activity-face-embedding-group-v1")
    public void consume(String message) {
        try {
            StudentFaceEmbeddingEvent event = readPayload(message);
            StudentFaceEmbeddingProjection projection = projectionService.upsert(event);
            log.info("[ACTIVITY] Student face embedding synchronized. userId={}, status={}, version={}",
                    projection.getUserId(), projection.getStatus(), projection.getEmbeddingVersion());
        } catch (Exception exception) {
            log.error("Cannot synchronize Student face embedding event into Activity Service: {}",
                    message, exception);
            throw new IllegalStateException("Student face embedding synchronization failed", exception);
        }
    }

    private StudentFaceEmbeddingEvent readPayload(String message)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.has("payload") ? root.get("payload") : root;
        return objectMapper.treeToValue(payload, StudentFaceEmbeddingEvent.class);
    }
}
