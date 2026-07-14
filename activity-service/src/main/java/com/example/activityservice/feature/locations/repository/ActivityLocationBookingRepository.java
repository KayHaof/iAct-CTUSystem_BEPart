package com.example.activityservice.feature.locations.repository;

import com.example.activityservice.feature.locations.model.ActivityLocationBooking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ActivityLocationBookingRepository extends JpaRepository<ActivityLocationBooking, Long> {

    List<ActivityLocationBooking> findByActivityId(Long activityId);

    void deleteByActivityId(Long activityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from ActivityLocationBooking b
            where b.location.id = :locationId
              and b.status in :statuses
              and (:excludedActivityId is null or b.activity.id <> :excludedActivityId)
              and :startTime < b.endTime
              and :endTime > b.startTime
            """)
    List<ActivityLocationBooking> findBlockingBookingsForUpdate(
            @Param("locationId") Long locationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<Integer> statuses,
            @Param("excludedActivityId") Long excludedActivityId);

    @Query("""
            select count(b) from ActivityLocationBooking b
            where b.location.id = :locationId
              and b.status in :statuses
              and :startTime < b.endTime
              and :endTime > b.startTime
            """)
    long countConflicts(
            @Param("locationId") Long locationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<Integer> statuses);

    @Query("""
            select b from ActivityLocationBooking b
            left join fetch b.activity
            left join fetch b.location
            left join fetch b.schedule
            where b.location.id = :locationId
              and (:statuses is null or b.status in :statuses)
              and :startTime < b.endTime
              and :endTime > b.startTime
            order by b.startTime asc, b.endTime asc
            """)
    List<ActivityLocationBooking> findScheduleByLocation(
            @Param("locationId") Long locationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<Integer> statuses);
}
