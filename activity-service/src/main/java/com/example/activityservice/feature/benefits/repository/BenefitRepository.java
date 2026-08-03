package com.example.activityservice.feature.benefits.repository;

import com.example.activityservice.feature.benefits.model.Benefits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BenefitRepository extends JpaRepository<Benefits, Long> {
    List<Benefits> findByActivityId(Long activityId);
    void deleteByActivityId(Long activityId);
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT b FROM Benefits b WHERE b.activity.semester.id = :semesterId")
    List<Benefits> findBySemesterId(@Param("semesterId") Long semesterId);

    @Query(value = """
        SELECT DISTINCT b.* FROM benefits b
        INNER JOIN activities a ON b.activity_id = a.id
        INNER JOIN registrations r ON r.activity_id = a.id
        INNER JOIN attendances at ON at.registration_id = r.id
        INNER JOIN proofs p ON p.registration_id = r.id
        INNER JOIN semesters s ON a.semester_id = s.id
        WHERE r.student_id = :studentId
          AND s.id = :semesterId
          AND at.checkin_time IS NOT NULL
          AND at.checkout_time IS NOT NULL
          AND p.status = 1
        """, nativeQuery = true)
    List<Benefits> findAwardedByStudentIdAndSemesterId(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);
}
