package com.example.userservice.feature.kafka;

import com.example.event.UserSnapshotEvent;
import com.example.event.kafka.KafkaEventEnvelope;
import com.example.event.kafka.KafkaEventMetadata;
import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.userservice.feature.preference.dto.PreferenceResponse;
import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDomainEventProducer {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishUserCreated(Long userId) {
        publishUserSnapshotEvent(userId, KafkaTopics.USER_CREATED, KafkaEventTypes.USER_CREATED, "user-lifecycle");
        publishUserSnapshotEvent(userId, KafkaTopics.PROFILE_CREATED, KafkaEventTypes.PROFILE_CREATED, "profile-lifecycle");
    }

    public void publishUserUpdated(Long userId) {
        publishUserSnapshotEvent(userId, KafkaTopics.USER_UPDATED, KafkaEventTypes.USER_UPDATED, "user-lifecycle");
    }

    public void publishUserDeactivated(Long userId) {
        publishUserSnapshotEvent(userId, KafkaTopics.USER_DEACTIVATED, KafkaEventTypes.USER_DEACTIVATED, "user-lifecycle");
    }

    public void publishUserDeleted(Long userId) {
        publishUserSnapshotEvent(userId, KafkaTopics.USER_DELETED, KafkaEventTypes.USER_DELETED, "user-lifecycle");
    }

    public void publishProfileUpdated(Long userId) {
        publishUserSnapshotEvent(userId, KafkaTopics.PROFILE_UPDATED, KafkaEventTypes.PROFILE_UPDATED, "profile-lifecycle");
    }

    public void publishPreferenceCreated(PreferenceResponse preference) {
        publishPreference(preference, KafkaTopics.PREFERENCE_CREATED, KafkaEventTypes.PREFERENCE_CREATED);
    }

    public void publishPreferenceUpdated(PreferenceResponse preference) {
        publishPreference(preference, KafkaTopics.PREFERENCE_UPDATED, KafkaEventTypes.PREFERENCE_UPDATED);
    }

    public void publishPreferenceDeleted(Long userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        publish(KafkaTopics.PREFERENCE_DELETED, KafkaEventTypes.PREFERENCE_DELETED, "preference",
                String.valueOf(userId), payload, "preference-lifecycle");
    }

    private void publishUserSnapshotEvent(Long userId, String topic, String eventType, String source) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        ProfileDto profile = userProfileService.getProfileByUserId(userId);
        UserSnapshotEvent payload = UserSnapshotEvent.builder()
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
        Map<String, Object> eventPayload = objectMapper.convertValue(payload, Map.class);
        eventPayload.put("status", user.getStatus());
        publish(topic, eventType, "user", String.valueOf(userId), eventPayload, source);
    }

    private void publishPreference(PreferenceResponse preference, String topic, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("preferenceId", preference.getId());
        payload.put("userId", preference.getUserId());
        payload.put("categoryRatings", preference.getCategoryRatings());
        payload.put("categoryEnabled", preference.getCategoryEnabled());
        payload.put("preferredCategoryIds", preference.getPreferredCategoryIds());
        payload.put("notificationSettings", preference.getNotificationSettings());
        payload.put("excludedCategories", preference.getExcludedCategories());
        payload.put("aiRecommendationEnabled", preference.getAiRecommendationEnabled());
        publish(topic, eventType, "preference", String.valueOf(preference.getUserId()),
                payload, "preference-lifecycle");
    }

    private void publish(String topic, String eventType, String aggregateType, String aggregateId,
                         Object payload, String source) {
        String eventId = UUID.randomUUID().toString();
        KafkaEventEnvelope<Object> envelope = KafkaEventEnvelope.builder()
                .eventId(eventId)
                .eventVersion(1)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .occurredAt(Instant.now().toString())
                .producer("user-service")
                .payload(payload)
                .metadata(KafkaEventMetadata.builder().source(source).build())
                .build();

        String message;
        try {
            message = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize user domain event", exception);
        }

        Runnable sendAction = () -> kafkaTemplate.send(topic, aggregateId, message)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Cannot send user domain event. topic={}, eventId={}, aggregateId={}",
                                topic, eventId, aggregateId, exception);
                    } else {
                        log.info("User domain event sent. topic={}, eventId={}, aggregateId={}",
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
