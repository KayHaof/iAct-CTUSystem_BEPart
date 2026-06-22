package com.example.activityservice.feature.proofs.repository;

import com.example.activityservice.feature.proofs.model.Proofs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProofRepository extends JpaRepository<Proofs, Long> {
    Optional<Proofs> findByStudentIdAndActivityId(Long studentId, Long activityId);
}