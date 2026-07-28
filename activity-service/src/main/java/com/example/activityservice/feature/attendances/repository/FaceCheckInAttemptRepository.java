package com.example.activityservice.feature.attendances.repository;

import com.example.activityservice.feature.attendances.model.FaceCheckInAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaceCheckInAttemptRepository extends JpaRepository<FaceCheckInAttempt, Long> {
    long countByRegistrationId(Long registrationId);

    Optional<FaceCheckInAttempt> findTopByRegistrationIdOrderByAttemptNoDesc(Long registrationId);
}
