package com.example.userservice.feature.preference.repository;

import com.example.userservice.feature.preference.model.StudentPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentPreferencesRepository extends JpaRepository<StudentPreferences, Long> {
    Optional<StudentPreferences> findByUserId(Long userId);
}
