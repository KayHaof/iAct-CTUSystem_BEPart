package com.example.activityservice.feature.certificate_submissions.model;

import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "certificate_submissions")
public class CertificateSubmission {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_CANCELLED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Users student;

    @Column(name = "student_code_snapshot", length = 50)
    private String studentCodeSnapshot;

    @Column(name = "student_name_snapshot")
    private String studentNameSnapshot;

    @Column(name = "department_id")
    private Long departmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "student_note", columnDefinition = "TEXT")
    private String studentNote;

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(name = "extracted_json", columnDefinition = "LONGTEXT")
    private String extractedJson;

    @Column(name = "extracted_student_name")
    private String extractedStudentName;

    @Column(name = "extracted_student_code", length = 50)
    private String extractedStudentCode;

    @Column(name = "certificate_title", length = 500)
    private String certificateTitle;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "achievement", columnDefinition = "TEXT")
    private String achievement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_category_id")
    private Categories suggestedCategory;

    @Column(name = "suggested_category_name")
    private String suggestedCategoryName;

    @Column(name = "suggested_point")
    private Integer suggestedPoint;

    @Column(name = "suggestion_reason", columnDefinition = "TEXT")
    private String suggestionReason;

    @Column(name = "ai_confidence", precision = 5, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "ai_warnings_json", columnDefinition = "LONGTEXT")
    private String aiWarningsJson;

    @Column(name = "needs_review")
    private Boolean needsReview = true;

    @Column(name = "status", nullable = false)
    private Integer status = STATUS_PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Users reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_category_id")
    private Categories approvedCategory;

    @Column(name = "approved_point")
    private Integer approvedPoint;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
