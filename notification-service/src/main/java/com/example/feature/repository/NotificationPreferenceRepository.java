package com.example.feature.repository;

import com.example.feature.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    boolean existsByUserIdAndTypeAndIsEnabledFalse(Long userId, Integer type);
    Optional<NotificationPreference> findByUserIdAndType(Long userId, Integer type);
    void deleteByUserId(Long userId);
}
