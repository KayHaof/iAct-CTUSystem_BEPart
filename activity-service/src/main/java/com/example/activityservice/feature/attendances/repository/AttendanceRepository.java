package com.example.activityservice.feature.attendances.repository;

import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.registration.model.Registrations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendances, Long> {
    Optional<Attendances> findFirstByRegistrationIdOrderByCheckinTimeAscIdAsc(Long registrationId);
    Optional<Attendances> findFirstByRegistrationIdAndScheduleIdOrderByIdAsc(Long registrationId, Long scheduleId);
    Optional<Attendances> findFirstByRegistrationIdAndScheduleIsNullOrderByIdAsc(Long registrationId);
    List<Attendances> findAllByRegistrationId(Long registrationId);
    long countByRegistrationId(Long registrationId);
    List<Attendances> findByRegistrationIn(List<Registrations> registrations);
    List<Attendances> findByRegistrationInAndScheduleId(List<Registrations> registrations, Long scheduleId);

    @Modifying
    @Query("""
            UPDATE Attendances attendance
            SET attendance.status = :absentStatus
            WHERE attendance.schedule IS NOT NULL
              AND attendance.schedule.endTime < :now
              AND attendance.checkinTime IS NULL
              AND (attendance.status IS NULL OR attendance.status = :pendingStatus)
            """)
    int markPendingAttendancesAbsent(
            @Param("now") LocalDateTime now,
            @Param("pendingStatus") Integer pendingStatus,
            @Param("absentStatus") Integer absentStatus);

    @Query("""
            SELECT registration, schedule
            FROM Registrations registration
            JOIN registration.registeredSchedules schedule
            WHERE (registration.status IS NULL OR registration.status <> 2)
              AND schedule.endTime < :now
              AND NOT EXISTS (
                  SELECT attendance.id
                  FROM Attendances attendance
                  WHERE attendance.registration = registration
                    AND attendance.schedule = schedule
              )
            """)
    List<Object[]> findMissingAttendanceRowsForEndedSchedules(@Param("now") LocalDateTime now);
}
