package com.example.feature.attendances.repository;

import com.example.feature.attendances.model.Attendances;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendances, Long> {
    Optional<Attendances> findByRegistrationId(Long registrationId);
    boolean existsByRegistrationId(Long registrationId);
}