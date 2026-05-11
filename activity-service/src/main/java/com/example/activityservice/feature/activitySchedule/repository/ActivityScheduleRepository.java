package com.example.activityservice.feature.activitySchedule.repository;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityScheduleRepository extends JpaRepository<ActivitySchedule, Long> {
}
