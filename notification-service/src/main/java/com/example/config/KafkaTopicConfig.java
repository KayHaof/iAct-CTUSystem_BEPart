package com.example.config;

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
    public NewTopic notificationCreatedTopic() {
        return topic(KafkaTopics.NOTIFICATION_CREATED);
    }

    @Bean
    public NewTopic notificationDispatchedTopic() {
        return topic(KafkaTopics.NOTIFICATION_DISPATCHED);
    }

    @Bean
    public NewTopic notificationReadTopic() {
        return topic(KafkaTopics.NOTIFICATION_READ);
    }

    @Bean
    public NewTopic notificationDeletedTopic() {
        return topic(KafkaTopics.NOTIFICATION_DELETED);
    }

    @Bean
    public NewTopic notificationCleanupRequestedTopic() {
        return topic(KafkaTopics.NOTIFICATION_CLEANUP_REQUESTED);
    }

    @Bean
    public NewTopic notificationBroadcastRequestedTopic() {
        return topic(KafkaTopics.NOTIFICATION_BROADCAST_REQUESTED);
    }

    @Bean
    public NewTopic notificationUrgentRequestedTopic() {
        return topic(KafkaTopics.NOTIFICATION_URGENT_REQUESTED);
    }

    @Bean
    public NewTopic notificationDeliveryFailedTopic() {
        return topic(KafkaTopics.NOTIFICATION_DELIVERY_FAILED);
    }

    @Bean
    public NewTopic systemDeadLetterTopic() {
        return topic(KafkaTopics.SYSTEM_DEAD_LETTER);
    }

    @Bean
    public NewTopic activityCreatedTopic() { return topic(KafkaTopics.ACTIVITY_CREATED); }

    @Bean
    public NewTopic activitySubmittedTopic() { return topic(KafkaTopics.ACTIVITY_SUBMITTED); }

    @Bean
    public NewTopic activityUpdatedTopic() { return topic(KafkaTopics.ACTIVITY_UPDATED); }

    @Bean
    public NewTopic activityApprovedTopic() { return topic(KafkaTopics.ACTIVITY_APPROVED); }

    @Bean
    public NewTopic activityRejectedTopic() { return topic(KafkaTopics.ACTIVITY_REJECTED); }

    @Bean
    public NewTopic activityCancelledTopic() { return topic(KafkaTopics.ACTIVITY_CANCELLED); }

    @Bean
    public NewTopic activityDeletedTopic() { return topic(KafkaTopics.ACTIVITY_DELETED); }

    @Bean
    public NewTopic activityDraftExpiredTopic() { return topic(KafkaTopics.ACTIVITY_DRAFT_EXPIRED); }

    @Bean
    public NewTopic registrationCreatedTopic() { return topic(KafkaTopics.REGISTRATION_CREATED); }

    @Bean
    public NewTopic registrationCancelledTopic() { return topic(KafkaTopics.REGISTRATION_CANCELLED); }

    @Bean
    public NewTopic attendanceCheckedInTopic() { return topic(KafkaTopics.ATTENDANCE_CHECKED_IN); }

    @Bean
    public NewTopic proofSubmittedTopic() { return topic(KafkaTopics.PROOF_SUBMITTED); }

    @Bean
    public NewTopic proofApprovedTopic() { return topic(KafkaTopics.PROOF_APPROVED); }

    @Bean
    public NewTopic proofRejectedTopic() { return topic(KafkaTopics.PROOF_REJECTED); }

    @Bean
    public NewTopic pointAwardedTopic() { return topic(KafkaTopics.POINT_AWARDED); }

    @Bean
    public NewTopic pointRecalculatedTopic() { return topic(KafkaTopics.POINT_RECALCULATED); }

    @Bean
    public NewTopic pointRevokedTopic() { return topic(KafkaTopics.POINT_REVOKED); }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
