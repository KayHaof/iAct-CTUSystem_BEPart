package com.example.activityservice.feature.complaints.repository;

import com.example.activityservice.feature.complaints.model.Complaints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaints, Long> {
    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Optional<Complaints> findByRegistrationId(Long registrationId);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    List<Complaints> findByRegistrationIdIn(Collection<Long> registrationIds);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByStatus(Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByActivityId(Long activityId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByActivityIdAndStatus(Long activityId, Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByActivityDepartmentId(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByActivityDepartmentIdAndStatus(Long departmentId, Integer status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByActivityIdAndActivityDepartmentId(Long activityId, Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student"
    })
    Page<Complaints> findByActivityIdAndActivityDepartmentIdAndStatus(
            Long activityId,
            Long departmentId,
            Integer status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "registration",
            "registration.activity",
            "registration.activity.semester",
            "registration.student",
            "activity",
            "semester",
            "student",
            "resolvedBy"
    })
    @Query("select c from Complaints c where c.id = :id")
    Optional<Complaints> findDetailById(@Param("id") Long id);
}
