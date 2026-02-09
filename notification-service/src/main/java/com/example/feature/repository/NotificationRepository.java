package com.example.feature.repository;

import com.example.feature.model.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notifications, Long> {

    List<Notifications> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    @Query("SELECT n FROM Notifications n LEFT JOIN FETCH n.activity WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notifications> findAllByUserIdFetched(@Param("userId") Long userId);
}