package com.example.activityservice.feature.certificate_submissions.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CertificateSubmissionResponse {
    private Long id;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long departmentId;
    private Long semesterId;
    private String semesterName;
    private String imageUrl;
    private String studentNote;
    private String rawText;
    private String extractedJson;
    private String extractedStudentName;
    private String extractedStudentCode;
    private String certificateTitle;
    private String issuer;
    private LocalDate issuedDate;
    private String achievement;
    private Long suggestedCategoryId;
    private String suggestedCategoryName;
    private Integer suggestedPoint;
    private String suggestionReason;
    private BigDecimal aiConfidence;
    private List<String> aiWarnings;
    private Boolean needsReview;
    private Integer status;
    private String statusLabel;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private Long approvedCategoryId;
    private String approvedCategoryName;
    private Integer approvedPoint;
    private String reviewNote;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
