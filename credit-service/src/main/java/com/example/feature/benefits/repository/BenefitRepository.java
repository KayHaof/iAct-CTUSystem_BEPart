package com.example.feature.benefits.repository;

import com.example.feature.benefits.model.Benefits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenefitRepository extends JpaRepository<Benefits, Long> {
    List<Benefits> findByActivityId(Long activityId);
}