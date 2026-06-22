package com.example.activityservice.feature.award_criteria.repository;

import com.example.activityservice.feature.award_criteria.model.Award_Criterias;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardCriteriaRepository extends JpaRepository<Award_Criterias, Long> {
    boolean existsByCategoryId(Long categoryId);
}
