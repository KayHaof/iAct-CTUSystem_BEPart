package com.example.activityservice.feature.registration.repository;

import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registrations, Long>, JpaSpecificationExecutor<Registrations> {

    boolean existsByStudentIdAndActivityId(Long studentId, Long activityId);

    Optional<Registrations> findByStudentIdAndActivityId(Long studentId, Long activityId);

    @Query("""
            SELECT DISTINCT r FROM Registrations r
            JOIN FETCH r.activity
            LEFT JOIN FETCH r.registeredSchedules
            WHERE r.student.id = :studentId
              AND (r.status IS NULL OR r.status <> :cancelledStatus)
            """)
    List<Registrations> findActiveRegistrationsWithSchedulesByStudentId(
            @Param("studentId") Long studentId,
            @Param("cancelledStatus") Integer cancelledStatus);

    @Query("""
            SELECT DISTINCT schedule FROM Registrations registration
            JOIN registration.registeredSchedules schedule
            WHERE registration.student.id = :studentId
              AND (registration.status IS NULL OR registration.status <> :cancelledStatus)
              AND registration.activity.id <> :targetActivityId
              AND schedule.startTime < :selectedEndTime
              AND schedule.endTime > :selectedStartTime
            """)
    List<ActivitySchedule> findOverlappingActiveSchedulesByStudentId(
            @Param("studentId") Long studentId,
            @Param("targetActivityId") Long targetActivityId,
            @Param("selectedStartTime") java.time.LocalDateTime selectedStartTime,
            @Param("selectedEndTime") java.time.LocalDateTime selectedEndTime,
            @Param("cancelledStatus") Integer cancelledStatus);

    long countByActivityIdAndStatusNot(Long activityId, Integer status);

    long countByActivityIdAndStatus(Long activityId, Integer status);

    List<Registrations> findAllByActivityId(Long activityId);

    @Query("""
            SELECT DISTINCT r.student.id FROM Registrations r
            WHERE r.activity.id = :activityId
              AND (r.status IS NULL OR r.status = 0)
              AND r.student.id IS NOT NULL
            """)
    List<Long> findRegisteredStudentIdsByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(r) FROM Registrations r WHERE r.activity.id = :activityId AND (r.status IS NULL OR r.status <> 2)")
    long countActiveRegistrationsByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(r) FROM Registrations r WHERE r.activity.id = :activityId AND r.status = 1 AND NOT EXISTS (SELECT p.id FROM Proofs p WHERE p.registration = r)")
    long countEligibleRegistrationsWithoutProof(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(r) FROM Registrations r WHERE r.activity.id = :activityId AND r.status = 3")
    long countAbsentRegistrationsByActivityId(@Param("activityId") Long activityId);

    @Query("""
            SELECT DISTINCT r, schedule
            FROM Registrations r
            JOIN r.registeredSchedules schedule
            WHERE r.activity.status = 1
              AND r.student.status = 1
              AND (r.status IS NULL OR r.status IN (0, 1))
              AND schedule.endTime > :now
              AND schedule.endTime <= :reminderThreshold
            """)
    List<Object[]> findActiveRegistrationsWithSchedulesEndingSoon(
            @Param("now") java.time.LocalDateTime now,
            @Param("reminderThreshold") java.time.LocalDateTime reminderThreshold);

    @Query("SELECT COUNT(r) FROM Registrations r WHERE r.activity.id = :activityId AND r.status = 3 AND (r.absenceReviewed IS NULL OR r.absenceReviewed = false)")
    long countUnreviewedAbsencesByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT r FROM Registrations r WHERE r.status IS NULL OR r.status = 0")
    List<Registrations> findRegistrationsForAbsenceScan();

    @Query("SELECT r FROM Registrations r WHERE r.activity.id = :activityId AND r.status = 1 AND NOT EXISTS (SELECT p.id FROM Proofs p WHERE p.registration = r)")
    Page<Registrations> findEligibleRegistrationsWithoutProof(
            @Param("activityId") Long activityId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r.student.academicYear FROM Registrations r
            WHERE r.activity.id = :activityId
              AND r.student.academicYear IS NOT NULL
              AND r.student.academicYear <> ''
            ORDER BY r.student.academicYear
            """)
    List<String> findDistinctAcademicYearsByActivityId(@Param("activityId") Long activityId);

    @Query("""
            SELECT DISTINCT r.student.id FROM Registrations r
            WHERE r.activity.id = :activityId
              AND r.student.id IS NOT NULL
              AND r.student.status = 1
              AND (r.status IS NULL OR r.status <> 2)
            """)
    List<Long> findActiveRegisteredStudentIdsByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(DISTINCT r.student.id) FROM Registrations r")
    long countDistinctStudentIds();

    @Query("""
            SELECT COUNT(DISTINCT r.student.id) FROM Registrations r
            WHERE r.activity.departmentId = :departmentId
              AND r.student.id IS NOT NULL
              AND (r.status IS NULL OR r.status <> 2)
            """)
    long countDistinctStudentIdsByActivityDepartmentId(@Param("departmentId") Long departmentId);

    @Query("""
            SELECT r FROM Registrations r
            JOIN FETCH r.activity a
            LEFT JOIN FETCH a.semester
            LEFT JOIN FETCH r.attendances at
            WHERE r.student.id = :studentId
              AND (:semesterId IS NULL OR a.semester.id = :semesterId)
              AND (
                    (r.status = 1 AND at.checkinTime IS NOT NULL)
                    OR (
                        (r.status IS NULL OR r.status <> 2)
                        AND (
                            SELECT COUNT(faceAttempt)
                            FROM FaceCheckInAttempt faceAttempt
                            WHERE faceAttempt.registration.id = r.id
                        ) >= 5
                        AND NOT EXISTS (
                            SELECT successfulAttempt.id
                            FROM FaceCheckInAttempt successfulAttempt
                            WHERE successfulAttempt.registration.id = r.id
                              AND (
                                  successfulAttempt.verified = true
                                  OR (
                                      successfulAttempt.distance IS NOT NULL
                                      AND successfulAttempt.threshold IS NOT NULL
                                      AND successfulAttempt.distance <= successfulAttempt.threshold
                                  )
                              )
                        )
                    )
              )
            ORDER BY r.registeredAt DESC
            """)
    List<Registrations> findComplaintEligibleRegistrations(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId);
}
