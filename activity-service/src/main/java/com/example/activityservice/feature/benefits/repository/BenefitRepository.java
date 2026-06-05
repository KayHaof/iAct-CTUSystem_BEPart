package com.example.activityservice.feature.benefits.repository;

import com.example.activityservice.feature.benefits.model.Benefits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BenefitRepository extends JpaRepository<Benefits, Long> {
    List<Benefits> findByActivityId(Long activityId);
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT b FROM Benefits b WHERE b.activity.semester.id = :semesterId")
    List<Benefits> findBySemesterId(@Param("semesterId") Long semesterId);

    @Query(value = """
        SELECT DISTINCT b.* FROM benefits b
        INNER JOIN activities a ON b.activity_id = a.id
        INNER JOIN registrations r ON r.activity_id = a.id
        INNER JOIN semesters s ON a.semester_id = s.id
        WHERE r.student_id = :studentId AND s.id = :semesterId
        """, nativeQuery = true)
    List<Benefits> findByStudentIdAndSemesterId(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);
}
