package com.example.activityservice.feature.certificate_submissions.mapper;

import com.example.activityservice.feature.certificate_submissions.dto.CertificateSubmissionResponse;
import com.example.activityservice.feature.certificate_submissions.model.CertificateSubmission;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CertificateSubmissionMapper {

    private final ObjectMapper objectMapper;

    public CertificateSubmissionResponse toResponse(CertificateSubmission entity) {
        if (entity == null) {
            return null;
        }

        return CertificateSubmissionResponse.builder()
                .id(entity.getId())
                .studentId(entity.getStudent() != null ? entity.getStudent().getId() : null)
                .studentCode(entity.getStudentCodeSnapshot())
                .studentName(entity.getStudentNameSnapshot())
                .departmentId(entity.getDepartmentId())
                .semesterId(entity.getSemester() != null ? entity.getSemester().getId() : null)
                .semesterName(entity.getSemester() != null ? entity.getSemester().getName() : null)
                .imageUrl(entity.getImageUrl())
                .studentNote(entity.getStudentNote())
                .rawText(entity.getRawText())
                .extractedJson(entity.getExtractedJson())
                .extractedStudentName(entity.getExtractedStudentName())
                .extractedStudentCode(entity.getExtractedStudentCode())
                .certificateTitle(entity.getCertificateTitle())
                .issuer(entity.getIssuer())
                .issuedDate(entity.getIssuedDate())
                .achievement(entity.getAchievement())
                .suggestedCategoryId(entity.getSuggestedCategory() != null ? entity.getSuggestedCategory().getId() : null)
                .suggestedCategoryName(entity.getSuggestedCategory() != null
                        ? entity.getSuggestedCategory().getName()
                        : entity.getSuggestedCategoryName())
                .suggestedPoint(entity.getSuggestedPoint())
                .suggestionReason(entity.getSuggestionReason())
                .aiConfidence(entity.getAiConfidence())
                .aiWarnings(readWarnings(entity.getAiWarningsJson()))
                .needsReview(entity.getNeedsReview())
                .status(entity.getStatus())
                .statusLabel(statusLabel(entity.getStatus()))
                .reviewerId(entity.getReviewer() != null ? entity.getReviewer().getId() : null)
                .reviewerName(entity.getReviewer() != null ? entity.getReviewer().getFullName() : null)
                .reviewedAt(entity.getReviewedAt())
                .approvedCategoryId(entity.getApprovedCategory() != null ? entity.getApprovedCategory().getId() : null)
                .approvedCategoryName(entity.getApprovedCategory() != null ? entity.getApprovedCategory().getName() : null)
                .approvedPoint(entity.getApprovedPoint())
                .reviewNote(entity.getReviewNote())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private List<String> readWarnings(String warningsJson) {
        if (warningsJson == null || warningsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(warningsJson, new TypeReference<>() {});
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String statusLabel(Integer status) {
        if (status == null || status == CertificateSubmission.STATUS_PENDING) {
            return "Chờ duyệt";
        }
        if (status == CertificateSubmission.STATUS_APPROVED) {
            return "Đã duyệt";
        }
        if (status == CertificateSubmission.STATUS_REJECTED) {
            return "Từ chối";
        }
        if (status == CertificateSubmission.STATUS_CANCELLED) {
            return "Đã hủy";
        }
        return "Không xác định";
    }
}
