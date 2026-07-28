package com.example.activityservice.feature.certificate_submission_complaints.repository;

import com.example.activityservice.feature.certificate_submission_complaints.model.CertificateSubmissionComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CertificateSubmissionComplaintRepository extends JpaRepository<CertificateSubmissionComplaint, Long> {

    @EntityGraph(attributePaths = {
            "certificateSubmission", "certificateSubmission.student", "certificateSubmission.semester", "reviewer"
    })
    @Query("SELECT complaint FROM CertificateSubmissionComplaint complaint WHERE complaint.id = :id")
    Optional<CertificateSubmissionComplaint> findDetailById(@Param("id") Long id);

    Optional<CertificateSubmissionComplaint> findByCertificateSubmissionId(Long certificateSubmissionId);

    boolean existsByCertificateSubmissionId(Long certificateSubmissionId);

    @EntityGraph(attributePaths = {
            "certificateSubmission", "certificateSubmission.student", "certificateSubmission.semester", "reviewer"
    })
    @Query("""
            SELECT complaint
            FROM CertificateSubmissionComplaint complaint
            WHERE complaint.certificateSubmission.student.id = :studentId
              AND (:semesterId IS NULL OR complaint.certificateSubmission.semester.id = :semesterId)
              AND (:status IS NULL OR complaint.status = :status)
            """)
    Page<CertificateSubmissionComplaint> findMyComplaints(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") Integer status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "certificateSubmission", "certificateSubmission.student", "certificateSubmission.semester", "reviewer"
    })
    @Query("""
            SELECT complaint
            FROM CertificateSubmissionComplaint complaint
            WHERE (:departmentId IS NULL OR complaint.certificateSubmission.departmentId = :departmentId)
              AND (:semesterId IS NULL OR complaint.certificateSubmission.semester.id = :semesterId)
              AND (:status IS NULL OR complaint.status = :status)
              AND (
                    :keyword IS NULL
                    OR LOWER(complaint.certificateSubmission.studentCodeSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(complaint.certificateSubmission.studentNameSnapshot) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(complaint.certificateSubmission.certificateTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(complaint.certificateSubmission.issuer) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(complaint.complaintReason) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<CertificateSubmissionComplaint> findReviewComplaints(
            @Param("departmentId") Long departmentId,
            @Param("semesterId") Long semesterId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
