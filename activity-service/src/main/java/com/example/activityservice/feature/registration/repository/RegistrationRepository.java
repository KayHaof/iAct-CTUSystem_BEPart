package com.example.activityservice.feature.registration.repository;

import com.example.activityservice.feature.registration.model.Registrations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registrations, Long>, JpaSpecificationExecutor<Registrations> {

    boolean existsByStudentIdAndActivityId(Long studentId, Long activityId);

    Optional<Registrations> findByStudentIdAndActivityId(Long studentId, Long activityId);

    long countByActivityIdAndStatusNot(Long activityId, Integer status);

    List<Registrations> findAllByActivityId(Long activityId);

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
            LEFT JOIN FETCH r.attendance at
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
                    )
              )
            ORDER BY r.registeredAt DESC
            """)
    List<Registrations> findComplaintEligibleRegistrations(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId);
}
