package com.example.feature.repository;

import com.example.feature.model.Notifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notifications, Long> {

    List<Notifications> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notifications> findByUserId(Long userId, Pageable pageable);
    
    Page<Notifications> findByUserIdAndIsRead(Long userId, Boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Query("SELECT n FROM Notifications n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notifications> findAllByUserIdFetched(@Param("userId") Long userId);

    boolean existsByActivityId(Long activityId);

    @Modifying
    @Query("DELETE FROM Notifications n WHERE n.activityId = :activityId")
    void deleteByActivityId(@Param("activityId") Long activityId);
    
    @Modifying
    @Query("UPDATE Notifications n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}