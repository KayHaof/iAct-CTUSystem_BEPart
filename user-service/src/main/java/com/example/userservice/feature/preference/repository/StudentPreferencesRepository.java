package com.example.userservice.feature.preference.repository;

import com.example.userservice.feature.preference.model.StudentPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentPreferencesRepository extends JpaRepository<StudentPreferences, Long> {
    Optional<StudentPreferences> findByUserId(Long userId);
}
