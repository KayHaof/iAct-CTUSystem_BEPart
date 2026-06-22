package com.example.activityservice.feature.attendances.repository;

import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.registration.model.Registrations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendances, Long> {
    Optional<Attendances> findByRegistrationId(Long registrationId);
    boolean existsByRegistrationId(Long registrationId);
    List<Attendances> findByRegistrationIn(List<Registrations> registrations);
}