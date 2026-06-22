package com.example.userservice.feature.users.service;

import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProjectionReplayServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserProjectionPublisher projectionPublisher;

    @InjectMocks
    private UserProjectionReplayService replayService;

    @Test
    void publishesEveryUserWithItsProfile() {
        Users departmentUser = Users.builder().id(4L).username("dept_cict").build();
        Users studentUser = Users.builder().id(9L).username("sv5").build();
        ProfileDto departmentProfile = ProfileDto.builder()
                .userId(4L)
                .fullName("Khoa CNTT và TT")
                .departmentId(1L)
                .build();

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(departmentUser, studentUser)));
        when(userProfileService.getProfilesBatch(List.of(4L, 9L)))
                .thenReturn(Map.of(4L, departmentProfile));

        long scheduled = replayService.replayAll();

        assertEquals(2L, scheduled);
        verify(projectionPublisher).publish(departmentUser, departmentProfile);
        verify(projectionPublisher).publish(studentUser, null);
    }
}
