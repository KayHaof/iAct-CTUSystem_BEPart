package com.example.common.repository;

import com.example.common.entity.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalNotificationRepository extends JpaRepository<Notifications, Long> {
    boolean existsByActivityId(Long activityId);
    void deleteByActivityId(Long activityId);
}
