package com.example.activityservice.feature.proofs.repository;

import com.example.activityservice.feature.proofs.model.Proofs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;

public interface ProofRepository extends JpaRepository<Proofs, Long> {
    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Optional<Proofs> findByRegistrationId(Long registrationId);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Optional<Proofs> findFirstByRegistrationIdOrderByCreatedAtDescIdDesc(Long registrationId);

    long countByRegistration_Activity_Id(Long activityId);

    long countByRegistration_Activity_IdAndStatus(Long activityId, Integer status);

    @Query("SELECT COUNT(DISTINCT proof.registration.student.id) FROM Proofs proof WHERE proof.registration.activity.id = :activityId")
    long countDistinctStudentsByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT DISTINCT proof.registration.id FROM Proofs proof WHERE proof.registration.id IN :registrationIds")
    Set<Long> findRegistrationIdsWithProofs(@Param("registrationIds") Collection<Long> registrationIds);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findByStatus(Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findByRegistration_Activity_Id(Long activityId, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findByRegistration_Activity_IdAndStatus(Long activityId, Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    @Query("""
            SELECT proof FROM Proofs proof
            WHERE proof.registration.activity.departmentId = :departmentId
            """)
    Page<Proofs> findByActivityDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    @Query("""
            SELECT proof FROM Proofs proof
            WHERE proof.registration.activity.departmentId = :departmentId
              AND proof.status = :status
            """)
    Page<Proofs> findByActivityDepartmentIdAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    @Query("""
            SELECT proof FROM Proofs proof
            WHERE proof.registration.activity.id = :activityId
              AND proof.registration.activity.departmentId = :departmentId
            """)
    Page<Proofs> findByActivityIdAndActivityDepartmentId(
            @Param("activityId") Long activityId,
            @Param("departmentId") Long departmentId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    @Query("""
            SELECT proof FROM Proofs proof
            WHERE proof.registration.activity.id = :activityId
              AND proof.registration.activity.departmentId = :departmentId
              AND proof.status = :status
            """)
    Page<Proofs> findByActivityIdAndActivityDepartmentIdAndStatus(
            @Param("activityId") Long activityId,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"registration", "registration.student", "registration.activity"})
    Page<Proofs> findAll(Pageable pageable);
}
