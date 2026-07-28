package com.example.activityservice.feature.certificate_submission_complaints.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateSubmissionComplaintResponse {
    private Long id;
    private Long submissionId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long departmentId;
    private Long semesterId;
    private String semesterName;
    private String imageUrl;
    private String certificateTitle;
    private String complaintReason;
    private Integer status;
    private String statusLabel;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
