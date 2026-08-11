package com.example.activityservice.feature.activitySchedule.repository;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityScheduleRepository extends JpaRepository<ActivitySchedule, Long> {
    List<ActivitySchedule> findByActivityId(Long activityId);

    Optional<ActivitySchedule> findByIdAndActivityId(Long id, Long activityId);
}
