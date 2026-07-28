package com.example.userservice.feature.user_profile.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentFaceEmbeddingResponse {
    private Long userId;
    private String referenceImageUrl;
    private String referenceImagePublicId;
    private String embeddingVector;
    private Integer vectorSize;
    private String modelName;
    private String detectorBackend;
    private String normalizationMethod;
    private String distanceMetric;
    private BigDecimal qualityScore;
    private BigDecimal faceConfidence;
    private Integer embeddingVersion;
    private Integer status;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime revokedAt;
    private String revokedReason;
}
