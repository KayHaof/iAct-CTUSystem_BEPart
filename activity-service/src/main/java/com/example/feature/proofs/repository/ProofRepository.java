package com.example.feature.proofs.repository;

import com.example.feature.proofs.model.Proofs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProofRepository extends JpaRepository<Proofs, Long> {
    Optional<Proofs> findByStudentIdAndActivityId(Long studentId, Long activityId);
}