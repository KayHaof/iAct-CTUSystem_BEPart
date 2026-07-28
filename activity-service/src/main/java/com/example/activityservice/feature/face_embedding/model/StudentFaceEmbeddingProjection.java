package com.example.activityservice.feature.face_embedding.model;

import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "student_face_embeddings",
        indexes = {
                @Index(name = "idx_student_face_status", columnList = "status"),
                @Index(name = "idx_student_face_model", columnList = "model_name, embedding_version"),
                @Index(name = "idx_student_face_updated_at", columnList = "updated_at")
        }
)
public class StudentFaceEmbeddingProjection {

    public static final int STATUS_ACTIVE = 1;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private Users user;

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

    @Column(name = "distance_metric", nullable = false, length = 50)
    private String distanceMetric = "cosine";

    @Column(name = "quality_score", precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "face_confidence", precision = 10, scale = 6)
    private BigDecimal faceConfidence;

    @Column(name = "embedding_version", nullable = false)
    private Integer embeddingVersion = 1;

    @Column(name = "status", nullable = false)
    private Integer status = STATUS_ACTIVE;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 500)
    private String revokedReason;
}
