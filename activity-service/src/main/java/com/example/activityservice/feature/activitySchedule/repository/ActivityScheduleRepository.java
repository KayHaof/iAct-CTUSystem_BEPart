package com.example.activityservice.feature.activitySchedule.repository;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityScheduleRepository extends JpaRepository<ActivitySchedule, Long> {
}
