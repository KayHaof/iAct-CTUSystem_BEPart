package com.example.activityservice.config;

import com.example.event.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic legacyNotificationCreatedTopic() {
        return topic(KafkaTopics.LEGACY_NOTIFICATION_CREATED);
    }

    @Bean
    public NewTopic legacyActivityDeletedTopic() {
        return topic(KafkaTopics.LEGACY_ACTIVITY_DELETED);
    }

    @Bean
    public NewTopic legacyIdentityUserCreatedTopic() {
        return topic(KafkaTopics.LEGACY_IDENTITY_USER_CREATED);
    }

    @Bean
    public NewTopic legacyIdentityUserSnapshotTopic() {
        return topic(KafkaTopics.LEGACY_IDENTITY_USER_SNAPSHOT);
    }

    @Bean
    public NewTopic activityCreatedTopic() {
        return topic(KafkaTopics.ACTIVITY_CREATED);
    }

    @Bean
    public NewTopic activityUpdatedTopic() {
        return topic(KafkaTopics.ACTIVITY_UPDATED);
    }

    @Bean
    public NewTopic activityDeletedTopic() {
        return topic(KafkaTopics.ACTIVITY_DELETED);
    }

    @Bean
    public NewTopic activitySubmittedTopic() {
        return topic(KafkaTopics.ACTIVITY_SUBMITTED);
    }

    @Bean
    public NewTopic activityApprovedTopic() {
        return topic(KafkaTopics.ACTIVITY_APPROVED);
    }

    @Bean
    public NewTopic activityRejectedTopic() {
        return topic(KafkaTopics.ACTIVITY_REJECTED);
    }

    @Bean
    public NewTopic activityCancelledTopic() {
        return topic(KafkaTopics.ACTIVITY_CANCELLED);
    }

    @Bean
    public NewTopic activityDraftExpiredTopic() {
        return topic(KafkaTopics.ACTIVITY_DRAFT_EXPIRED);
    }

    @Bean
    public NewTopic registrationCreatedTopic() {
        return topic(KafkaTopics.REGISTRATION_CREATED);
    }

    @Bean
    public NewTopic registrationCancelledTopic() {
        return topic(KafkaTopics.REGISTRATION_CANCELLED);
    }

    @Bean
    public NewTopic attendanceCheckedInTopic() {
        return topic(KafkaTopics.ATTENDANCE_CHECKED_IN);
    }

    @Bean
    public NewTopic proofSubmittedTopic() {
        return topic(KafkaTopics.PROOF_SUBMITTED);
    }

    @Bean
    public NewTopic proofApprovedTopic() {
        return topic(KafkaTopics.PROOF_APPROVED);
    }

    @Bean
    public NewTopic proofRejectedTopic() {
        return topic(KafkaTopics.PROOF_REJECTED);
    }

    @Bean
    public NewTopic certificateSubmissionSubmittedTopic() {
        return topic(KafkaTopics.CERTIFICATE_SUBMISSION_SUBMITTED);
    }

    @Bean
    public NewTopic certificateSubmissionApprovedTopic() {
        return topic(KafkaTopics.CERTIFICATE_SUBMISSION_APPROVED);
    }

    @Bean
    public NewTopic certificateSubmissionRejectedTopic() {
        return topic(KafkaTopics.CERTIFICATE_SUBMISSION_REJECTED);
    }

    @Bean
    public NewTopic pointAwardedTopic() {
        return topic(KafkaTopics.POINT_AWARDED);
    }

    @Bean
    public NewTopic pointRecalculatedTopic() {
        return topic(KafkaTopics.POINT_RECALCULATED);
    }

    @Bean
    public NewTopic pointRevokedTopic() {
        return topic(KafkaTopics.POINT_REVOKED);
    }

    @Bean
    public NewTopic notificationCommandCreatedTopic() {
        return topic(KafkaTopics.NOTIFICATION_CREATED);
    }

    @Bean
    public NewTopic notificationBroadcastRequestedTopic() {
        return topic(KafkaTopics.NOTIFICATION_BROADCAST_REQUESTED);
    }

    @Bean
    public NewTopic notificationActivitySessionActionReminderRequestedTopic() {
        return topic(KafkaTopics.NOTIFICATION_ACTIVITY_SESSION_ACTION_REMINDER_REQUESTED);
    }

    @Bean
    public NewTopic notificationAbsenceViolationProcessedRequestedTopic() {
        return topic(KafkaTopics.NOTIFICATION_ABSENCE_VIOLATION_PROCESSED_REQUESTED);
    }

    @Bean
    public NewTopic standardUserSnapshotTopic() {
        return topic(KafkaTopics.USER_SNAPSHOT);
    }

    @Bean
    public NewTopic studentFaceEmbeddingSnapshotTopic() {
        return topic(KafkaTopics.STUDENT_FACE_EMBEDDING_SNAPSHOT);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
