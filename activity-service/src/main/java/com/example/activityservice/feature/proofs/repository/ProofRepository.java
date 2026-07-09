package com.example.activityservice.feature.proofs.repository;

import com.example.activityservice.feature.proofs.model.Proofs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProofRepository extends JpaRepository<Proofs, Long> {
    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Optional<Proofs> findByRegistrationId(Long registrationId);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findByStatus(Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findByRegistration_Activity_Id(Long activityId, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findByRegistration_Activity_IdAndStatus(Long activityId, Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findAll(Pageable pageable);
}
