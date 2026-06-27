package com.example.userservice.feature.users.service;

import com.example.event.UserSnapshotEvent;
import com.example.event.kafka.KafkaEventEnvelope;
import com.example.event.kafka.KafkaEventMetadata;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProjectionPublisher {

    public static final String USER_SNAPSHOT_TOPIC = KafkaTopics.LEGACY_IDENTITY_USER_SNAPSHOT;
    public static final String STANDARD_USER_SNAPSHOT_TOPIC = KafkaTopics.USER_SNAPSHOT;

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishById(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        publish(user, userProfileService.getProfileByUserId(userId));
    }

    public void publish(Users user, ProfileDto profile) {
        UserSnapshotEvent event = UserSnapshotEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventVersion(1)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(profile != null ? profile.getFullName() : null)
                .studentCode(profile != null ? profile.getStudentCode() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .departmentId(profile != null ? profile.getDepartmentId() : null)
                .occurredAt(Instant.now().toString())
                .build();

        String legacyPayload = serialize(event, "Cannot serialize User snapshot event");
        String standardPayload = serialize(toStandardEnvelope(user.getId(), event),
                "Cannot serialize standardized User snapshot event");

        Runnable sendAction = () -> send(user.getId(), event.getEventId(), legacyPayload, standardPayload);
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

    private KafkaEventEnvelope<UserSnapshotEvent> toStandardEnvelope(Long userId, UserSnapshotEvent event) {
        return KafkaEventEnvelope.<UserSnapshotEvent>builder()
                .eventId(event.getEventId())
                .eventVersion(1)
                .eventType(KafkaEventTypes.USER_SNAPSHOT)
                .aggregateType("user")
                .aggregateId(String.valueOf(userId))
                .occurredAt(event.getOccurredAt())
                .producer("user-service")
                .payload(event)
                .metadata(KafkaEventMetadata.builder().source("user-projection").build())
                .build();
    }

    private String serialize(Object payload, String failureMessage) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private void send(Long userId, String eventId, String legacyPayload, String standardPayload) {
        sendToTopic(USER_SNAPSHOT_TOPIC, userId, eventId, legacyPayload);
        sendToTopic(STANDARD_USER_SNAPSHOT_TOPIC, userId, eventId, standardPayload);
    }

    private void sendToTopic(String topic, Long userId, String eventId, String payload) {
        kafkaTemplate.send(topic, String.valueOf(userId), payload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Cannot send User snapshot. topic={}, eventId={}, userId={}",
                                topic, eventId, userId, exception);
                    } else {
                        log.info("User snapshot sent. topic={}, eventId={}, userId={}",
                                topic, eventId, userId);
                    }
                });
    }
}

