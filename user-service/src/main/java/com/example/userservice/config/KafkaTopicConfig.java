package com.example.userservice.config;

import com.example.event.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic legacyUserCreatedTopic() {
        return topic(KafkaTopics.LEGACY_IDENTITY_USER_CREATED);
    }

    @Bean
    public NewTopic legacyUserSnapshotTopic() {
        return topic(KafkaTopics.LEGACY_IDENTITY_USER_SNAPSHOT);
    }

    @Bean
    public NewTopic userCreatedTopic() {
        return topic(KafkaTopics.USER_CREATED);
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return topic(KafkaTopics.USER_UPDATED);
    }

    @Bean
    public NewTopic userDeactivatedTopic() {
        return topic(KafkaTopics.USER_DEACTIVATED);
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return topic(KafkaTopics.USER_DELETED);
    }

    @Bean
    public NewTopic userRoleChangedTopic() {
        return topic(KafkaTopics.USER_ROLE_CHANGED);
    }

    @Bean
    public NewTopic userSnapshotTopic() {
        return topic(KafkaTopics.USER_SNAPSHOT);
    }

    @Bean
    public NewTopic profileCreatedTopic() {
        return topic(KafkaTopics.PROFILE_CREATED);
    }

    @Bean
    public NewTopic profileUpdatedTopic() {
        return topic(KafkaTopics.PROFILE_UPDATED);
    }

    @Bean
    public NewTopic studentFaceEmbeddingSnapshotTopic() {
        return topic(KafkaTopics.STUDENT_FACE_EMBEDDING_SNAPSHOT);
    }

    @Bean
    public NewTopic preferenceCreatedTopic() {
        return topic(KafkaTopics.PREFERENCE_CREATED);
    }

    @Bean
    public NewTopic preferenceUpdatedTopic() {
        return topic(KafkaTopics.PREFERENCE_UPDATED);
    }

    @Bean
    public NewTopic preferenceDeletedTopic() {
        return topic(KafkaTopics.PREFERENCE_DELETED);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
