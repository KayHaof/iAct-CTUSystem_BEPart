package com.example.feature.activities.repository;

import com.example.feature.activities.model.Activities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activities, Long>, JpaSpecificationExecutor<Activities> {
    List<Activities> findByStatusAndUpdatedAtBefore(Integer status, LocalDateTime cutoffDate);
    List<Activities> findByStatus(Integer status);
    long countByStatus(Integer status);
}
