package com.example.activityservice.feature.activities.kafka;

import com.example.activityservice.feature.users.service.LocalUserProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityUserSyncConsumerTest {

    @Test
    void propagatesProjectionFailureForKafkaRetry() {
        LocalUserProjectionService projectionService = mock(LocalUserProjectionService.class);
        ActivityUserSyncConsumer consumer = new ActivityUserSyncConsumer(
                projectionService,
                new ObjectMapper());
        when(projectionService.upsert(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> consumer.consumeUserCreated(
                "{\"userId\":13,\"username\":\"sv1\"}"));
        verify(projectionService).upsert(any());
    }
}
