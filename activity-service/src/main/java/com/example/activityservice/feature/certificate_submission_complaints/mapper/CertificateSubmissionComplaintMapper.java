package com.example.activityservice.feature.certificate_submission_complaints.mapper;

import com.example.activityservice.feature.certificate_submission_complaints.dto.CertificateSubmissionComplaintResponse;
import com.example.activityservice.feature.certificate_submission_complaints.model.CertificateSubmissionComplaint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CertificateSubmissionComplaintMapper {

    public CertificateSubmissionComplaintResponse toResponse(CertificateSubmissionComplaint entity) {
        if (entity == null) {
            return null;
        }

        return CertificateSubmissionComplaintResponse.builder()
                .id(entity.getId())
                .submissionId(
                        entity.getCertificateSubmission() != null ? entity.getCertificateSubmission().getId() : null)
                .studentId(entity.getCertificateSubmission() != null
                        && entity.getCertificateSubmission().getStudent() != null
                                ? entity.getCertificateSubmission().getStudent().getId()
                                : null)
                .studentCode(entity.getCertificateSubmission() != null
                        ? entity.getCertificateSubmission().getStudentCodeSnapshot()
                        : null)
                .studentName(entity.getCertificateSubmission() != null
                        ? entity.getCertificateSubmission().getStudentNameSnapshot()
                        : null)
                .departmentId(
                        entity.getCertificateSubmission() != null ? entity.getCertificateSubmission().getDepartmentId()
                                : null)
                .semesterId(entity.getCertificateSubmission() != null
                        && entity.getCertificateSubmission().getSemester() != null
                                ? entity.getCertificateSubmission().getSemester().getId()
                                : null)
                .semesterName(entity.getCertificateSubmission() != null
                        && entity.getCertificateSubmission().getSemester() != null
                                ? entity.getCertificateSubmission().getSemester().getName()
                                : null)
                .imageUrl(entity.getCertificateSubmission() != null ? entity.getCertificateSubmission().getImageUrl()
                        : null)
                .certificateTitle(entity.getCertificateSubmission() != null
                        ? entity.getCertificateSubmission().getCertificateTitle()
                        : null)
                .complaintReason(entity.getComplaintReason())
                .status(entity.getStatus())
                .statusLabel(statusLabel(entity.getStatus()))
                .reviewerId(entity.getReviewer() != null ? entity.getReviewer().getId() : null)
                .reviewerName(entity.getReviewer() != null ? entity.getReviewer().getFullName() : null)
                .reviewedAt(entity.getReviewedAt())
                .reviewNote(entity.getReviewNote())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String statusLabel(Integer status) {
        if (status == null || status == CertificateSubmissionComplaint.STATUS_PENDING) {
            return "Chờ xử lý";
        }
        if (status == CertificateSubmissionComplaint.STATUS_APPROVED) {
            return "Đã duyệt";
        }
        if (status == CertificateSubmissionComplaint.STATUS_REJECTED) {
            return "Bị từ chối";
        }
        return "Không xác định";
    }
}
