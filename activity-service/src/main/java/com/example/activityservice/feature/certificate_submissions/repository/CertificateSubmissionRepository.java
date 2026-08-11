package com.example.activityservice.feature.certificate_submissions.repository;

import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CertificateSubmissionRepository extends JpaRepository<CertificateSubmission, Long> {

    @EntityGraph(attributePaths = {
            "student", "semester", "suggestedCategory", "approvedCategory", "reviewer"
    })
    @Query("SELECT submission FROM CertificateSubmission submission WHERE submission.id = :id")
    Optional<CertificateSubmission> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "student", "semester", "suggestedCategory", "approvedCategory", "reviewer"
    })
    @Query("""
            SELECT submission
            FROM CertificateSubmission submission
            WHERE submission.student.id = :studentId
              AND (:semesterId IS NULL OR submission.semester.id = :semesterId)
              AND (:status IS NULL OR submission.status = :status)
            """)
    Page<CertificateSubmission> findStudentSubmissions(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") Integer status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "student", "semester", "suggestedCategory", "approvedCategory", "reviewer"
    })
    @Query("""
            SELECT submission
            FROM CertificateSubmission submission
            WHERE (:departmentId IS NULL OR submission.departmentId = :departmentId)
              AND (:semesterId IS NULL OR submission.semester.id = :semesterId)
              AND (:status IS NULL OR submission.status = :status)
              AND (:excludeAutoRejected = false OR submission.status <> 2 OR submission.reviewer IS NOT NULL)
              AND (
                    :keyword IS NULL
                    OR LOWER(submission.studentCodeSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(submission.studentNameSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(submission.certificateTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(submission.extractedStudentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(submission.extractedStudentName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<CertificateSubmission> findReviewSubmissions(
            @Param("departmentId") Long departmentId,
            @Param("semesterId") Long semesterId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("excludeAutoRejected") boolean excludeAutoRejected,
            Pageable pageable);

    @EntityGraph(attributePaths = {"approvedCategory"})
    @Query("""
            SELECT submission
            FROM CertificateSubmission submission
            WHERE submission.student.id = :studentId
              AND submission.semester.id = :semesterId
              AND submission.status = 1
              AND submission.approvedCategory IS NOT NULL
              AND submission.approvedPoint > 0
            """)
    List<CertificateSubmission> findApprovedForPointSummary(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId);
}
