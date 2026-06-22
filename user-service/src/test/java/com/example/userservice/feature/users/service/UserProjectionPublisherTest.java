package com.example.userservice.feature.users.service;

import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProjectionPublisherTest {

    @Test
    void publishesVersionedSnapshotWithProfileData() {
        UserRepository userRepository = mock(UserRepository.class);
        UserProfileService profileService = mock(UserProfileService.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        UserProjectionPublisher publisher = new UserProjectionPublisher(
                userRepository, profileService, kafkaTemplate, new ObjectMapper());
        Users user = Users.builder()
                .id(4L)
                .username("dept_cict")
                .email("dept_cict@iact.com")
                .build();
        ProfileDto profile = ProfileDto.builder()
                .fullName("Khoa CNTT và TT")
                .departmentId(1L)
                .build();
        when(kafkaTemplate.send(
                eq(UserProjectionPublisher.USER_SNAPSHOT_TOPIC),
                eq("4"),
                contains("\"username\":\"dept_cict\"")))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(user, profile);

        verify(kafkaTemplate).send(
                eq(UserProjectionPublisher.USER_SNAPSHOT_TOPIC),
                eq("4"),
                contains("\"departmentId\":1"));
    }
}
