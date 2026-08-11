package com.example.activityservice.feature.registration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationKafkaProducer {

    private final RegistrationEventProducer registrationEventProducer;

    @Async
    public void sendRegistrationSuccess(Long userId, Long activityId, String activityTitle, String message) {
        try {
            registrationEventProducer.publishRegistrationCreated(userId, activityId, activityTitle);
            log.info("Kafka: Registration created event sent for userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send registration created event: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendCancellationSuccess(Long userId, Long activityId, String activityTitle, String reason) {
        try {
            registrationEventProducer.publishRegistrationCancelled(userId, activityId, activityTitle, reason);
            log.info("Kafka: Registration cancelled event sent for userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send registration cancelled event: {}", e.getMessage(), e);
        }
    }

    public void sendAbsenceViolationProcessed(
            Long userId,
            Long registrationId,
            Long activityId,
            String activityTitle,
            String referenceType,
            String title,
            String message,
            java.time.LocalDateTime processedAt) {
        try {
            registrationEventProducer.publishAbsenceViolationProcessed(
                    userId,
                    registrationId,
                    activityId,
                    activityTitle,
                    referenceType,
                    title,
                    message,
                    processedAt);
            log.info("Kafka: Absence violation notification requested for userId={}, registrationId={}",
                    userId, registrationId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send absence violation notification: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendCheckInSuccess(Long userId, Long activityId, String activityTitle, String sessionTitle) {
        try {
            registrationEventProducer.publishAttendanceCheckedIn(userId, activityId, activityTitle, sessionTitle);
            log.info("Kafka: Attendance checked-in event sent for userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            log.error("Kafka: Failed to send attendance checked-in event: {}", e.getMessage(), e);
        }
    }
}
