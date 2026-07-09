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
import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activities, Long>, JpaSpecificationExecutor<Activities> {
    List<Activities> findByStatusAndUpdatedAtBefore(Integer status, LocalDateTime cutoffDate);
    List<Activities> findByStatus(Integer status);
    long countByStatus(Integer status);
    boolean existsBySemesterId(Long semesterId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Activities a WHERE a.id = :id")
    Optional<Activities> findByIdForRegistrationUpdate(@Param("id") Long id);
    
    // New methods for UC features
    List<Activities> findByDepartmentId(Long departmentId);
    
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
}
