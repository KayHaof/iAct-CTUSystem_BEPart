package com.example.activityservice.feature.activities.repository;

import com.example.activityservice.feature.activities.model.Activities;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activities, Long>, JpaSpecificationExecutor<Activities> {
    interface DepartmentStatusCountProjection {
        Long getDepartmentId();

        Integer getStatus();

        Long getTotal();
    }

    List<Activities> findByStatusAndUpdatedAtBefore(Integer status, LocalDateTime cutoffDate);
    List<Activities> findByStatus(Integer status);
    long countByStatus(Integer status);
    long countByDepartmentId(Long departmentId);
    long countByDepartmentIdAndStatus(Long departmentId, Integer status);
    boolean existsBySemesterId(Long semesterId);
    Page<Activities> findByCreatedById(Long createdById, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Activities a WHERE a.id = :id")
    Optional<Activities> findByIdForRegistrationUpdate(@Param("id") Long id);
    
    // New methods for UC features
    List<Activities> findByDepartmentId(Long departmentId);

    @Query("""
            SELECT a FROM Activities a
            LEFT JOIN FETCH a.createdBy
            WHERE a.status = 1
              AND a.registrationStart IS NOT NULL
              AND a.registrationEnd IS NOT NULL
              AND a.registrationStart <= :now
              AND a.registrationEnd >= :now
            """)
    List<Activities> findApprovedActivitiesOpenForRegistration(@Param("now") LocalDateTime now);

    @Query("""
            SELECT a FROM Activities a
            WHERE a.status = 1
              AND a.startDate IS NOT NULL
              AND a.startDate > :now
              AND a.startDate <= :reminderThreshold
            """)
    List<Activities> findApprovedActivitiesStartingSoon(
            @Param("now") LocalDateTime now,
            @Param("reminderThreshold") LocalDateTime reminderThreshold);
    
    @Query(value = """
        SELECT DISTINCT a FROM Activities a
        LEFT JOIN FETCH a.organizer
        LEFT JOIN FETCH a.semester
        WHERE a.status = 1
        AND a.semester.id = :semesterId
        ORDER BY a.startDate ASC
        """)
    List<Activities> findApprovedActivitiesForStudent(@Param("semesterId") Long semesterId);
    
    @Query("SELECT a FROM Activities a LEFT JOIN FETCH a.organizer ORDER BY a.updatedAt DESC")
    Page<Activities> findRecentActivitiesWithOrganizer(Pageable pageable);

    @Query("""
            SELECT a FROM Activities a
            LEFT JOIN FETCH a.organizer
            WHERE a.departmentId = :departmentId
            ORDER BY a.updatedAt DESC
            """)
    Page<Activities> findRecentActivitiesWithOrganizerByDepartmentId(
            @Param("departmentId") Long departmentId,
            Pageable pageable);

    @Query("""
            SELECT a.departmentId AS departmentId,
                   a.status AS status,
                   COUNT(a) AS total
            FROM Activities a
            JOIN a.createdBy creator
            WHERE creator.roleType = :roleType
              AND a.status IN :statuses
            GROUP BY a.departmentId, a.status
            """)
    List<DepartmentStatusCountProjection> countByCreatorRoleTypeAndStatusesGroupedByDepartment(
            @Param("roleType") Integer roleType,
            @Param("statuses") Collection<Integer> statuses);

    @Query("""
            SELECT a.departmentId AS departmentId,
                   a.status AS status,
                   COUNT(a) AS total
            FROM Activities a
            JOIN a.createdBy creator
            WHERE creator.roleType = :roleType
              AND a.requiresAdminApproval = :requiresAdminApproval
              AND a.status IN :statuses
            GROUP BY a.departmentId, a.status
            """)
    List<DepartmentStatusCountProjection> countByCreatorRoleTypeAndRequiresAdminApprovalAndStatusesGroupedByDepartment(
            @Param("roleType") Integer roleType,
            @Param("requiresAdminApproval") Boolean requiresAdminApproval,
            @Param("statuses") Collection<Integer> statuses);
}
