package com.example.activityservice.feature.users.service;

import com.example.activityservice.feature.users.dto.UserSnapshot;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalUserProjectionService {

    private final UserRepository userRepository;

    @Transactional
    public Users upsert(UserSnapshot snapshot) {
        Long userId = snapshot.resolvedId();
        if (userId == null || snapshot.getUsername() == null || snapshot.getUsername().isBlank()) {
            throw new IllegalArgumentException("User snapshot must contain userId and username");
        }

        Users user = userRepository.findById(userId).orElseGet(Users::new);
        user.setId(userId);
        user.setUsername(snapshot.getUsername());
        setIfPresent(snapshot.getEmail(), user::setEmail);
        setIfPresent(snapshot.getFullName(), user::setFullName);
        setIfPresent(snapshot.getStudentCode(), user::setStudentCode);
        setIfPresent(snapshot.getAvatarUrl(), user::setAvatarUrl);
        user.setClassId(snapshot.getClassId());
        user.setClassCode(snapshot.getClassCode());
        user.setClassName(snapshot.getClassName());
        user.setAcademicYear(snapshot.getAcademicYear());
        user.setDepartmentId(snapshot.getDepartmentId());
        if (snapshot.getRoleType() != null) {
            user.setRoleType(snapshot.getRoleType());
        }
        user.setStatus(snapshot.getStatus() != null ? snapshot.getStatus() : 1);
        return userRepository.save(user);
    }

    @Transactional
    public void markInactive(Long userId, Integer status) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }

    private void setIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
