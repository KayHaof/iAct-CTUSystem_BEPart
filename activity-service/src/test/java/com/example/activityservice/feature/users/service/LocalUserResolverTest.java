package com.example.activityservice.feature.users.service;

import com.example.activityservice.feature.users.dto.UserSnapshot;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.dto.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalUserResolverTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsLocalUserWithoutCallingUserService() {
        UserRepository repository = mock(UserRepository.class);
        LocalUserProjectionService projectionService = mock(LocalUserProjectionService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        LocalUserResolver resolver = createResolver(repository, projectionService, restTemplate);
        Users localUser = new Users();
        localUser.setId(13L);
        when(repository.findById(13L)).thenReturn(Optional.of(localUser));

        assertSame(localUser, resolver.resolveById(13L));

        verify(restTemplate, never()).exchange(
                any(String.class), any(HttpMethod.class), anyHttpEntity(),
                anyUserSnapshotResponseType());
    }

    @Test
    void fetchesAndProjectsUserWhenLocalProjectionIsMissing() {
        UserRepository repository = mock(UserRepository.class);
        LocalUserProjectionService projectionService = mock(LocalUserProjectionService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        LocalUserResolver resolver = createResolver(repository, projectionService, restTemplate);
        UserSnapshot snapshot = new UserSnapshot();
        snapshot.setId(13L);
        snapshot.setUsername("sv1");
        Users projected = new Users();
        projected.setId(13L);

        when(repository.findById(13L)).thenReturn(Optional.empty());
        when(restTemplate.exchange(
                eq("http://localhost:8081/api/v1/users/13"),
                eq(HttpMethod.GET),
                anyHttpEntity(),
                anyUserSnapshotResponseType()))
                .thenReturn(ResponseEntity.ok(ApiResponse.success(snapshot)));
        when(projectionService.upsert(snapshot)).thenReturn(projected);

        assertSame(projected, resolver.resolveById(13L));
        verify(projectionService).upsert(snapshot);
    }

    private LocalUserResolver createResolver(
            UserRepository repository,
            LocalUserProjectionService projectionService,
            RestTemplate restTemplate) {
        LocalUserResolver resolver = new LocalUserResolver(repository, projectionService, restTemplate);
        ReflectionTestUtils.setField(resolver, "userServiceUrl", "http://localhost:8081");
        return resolver;
    }

    private static HttpEntity<?> anyHttpEntity() {
        return any();
    }

    private static ParameterizedTypeReference<ApiResponse<UserSnapshot>> anyUserSnapshotResponseType() {
        return any();
    }
}
