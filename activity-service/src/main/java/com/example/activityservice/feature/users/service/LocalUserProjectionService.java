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
        if (snapshot.getDepartmentId() != null) {
            user.setDepartmentId(snapshot.getDepartmentId());
        }
        return userRepository.save(user);
    }

    private void setIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
