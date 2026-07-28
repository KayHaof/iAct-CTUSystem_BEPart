package com.example.userservice.feature.user_profile.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_face_embeddings",
        indexes = {
                @Index(name = "idx_student_face_status", columnList = "status"),
                @Index(name = "idx_student_face_model", columnList = "model_name, embedding_version"),
                @Index(name = "idx_student_face_updated_at", columnList = "updated_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFaceEmbedding {

    public static final int STATUS_INACTIVE = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_REVOKED = 2;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private StudentProfile studentProfile;

    @Column(name = "reference_image_url", nullable = false, length = 500)
    private String referenceImageUrl;

    @Column(name = "reference_image_public_id", length = 255)
    private String referenceImagePublicId;

    @Column(name = "embedding_vector", nullable = false, columnDefinition = "JSON")
    private String embeddingVector;

    @Column(name = "vector_size", nullable = false)
    private Integer vectorSize;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "detector_backend", length = 100)
    private String detectorBackend;

    @Column(name = "normalization_method", length = 100)
    private String normalizationMethod;

    @Builder.Default
    @Column(name = "distance_metric", nullable = false, length = 50)
    private String distanceMetric = "cosine";

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "face_confidence", precision = 10, scale = 6)
    private BigDecimal faceConfidence;

    @Builder.Default
    @Column(name = "embedding_version", nullable = false)
    private Integer embeddingVersion = 1;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private Integer status = STATUS_ACTIVE;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;
}
