package com.example.userservice.feature.users.service;

import com.example.event.UserSnapshotEvent;
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

    public static final String USER_SNAPSHOT_TOPIC = "iact.identity.user.snapshot";

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

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize User snapshot event", exception);
        }

        Runnable sendAction = () -> send(user.getId(), payload);
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

    private void send(Long userId, String payload) {
        kafkaTemplate.send(USER_SNAPSHOT_TOPIC, String.valueOf(userId), payload)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Không thể gửi User snapshot cho ID: {}", userId, exception);
                    } else {
                        log.info("Đã gửi User snapshot cho ID: {}", userId);
                    }
                });
    }
}
