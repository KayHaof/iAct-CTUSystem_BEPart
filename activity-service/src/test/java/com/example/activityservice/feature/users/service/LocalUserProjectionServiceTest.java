package com.example.activityservice.feature.users.service;

import com.example.activityservice.feature.users.dto.UserSnapshot;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalUserProjectionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LocalUserProjectionService projectionService;

    @Test
    void upsertPreservesOptionalValuesMissingFromEvent() {
        Users existing = new Users();
        existing.setId(13L);
        existing.setUsername("old-name");
        existing.setFullName("Tên đầy đủ");
        existing.setDepartmentId(2L);

        UserSnapshot snapshot = new UserSnapshot();
        snapshot.setUserId(13L);
        snapshot.setUsername("new-name");
        snapshot.setRoleType(1);

        when(userRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        Users saved = projectionService.upsert(snapshot);

        assertEquals("new-name", saved.getUsername());
        assertEquals("Tên đầy đủ", saved.getFullName());
        assertEquals(2L, saved.getDepartmentId());
        assertEquals(1, saved.getRoleType());
    }
}
