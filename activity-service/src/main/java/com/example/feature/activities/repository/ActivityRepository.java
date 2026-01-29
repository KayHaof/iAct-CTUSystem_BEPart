package com.example.feature.activities.repository;

import com.example.feature.activities.model.Activities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activities, Long> {
}
