package com.example.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentFaceEmbeddingEvent {
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
    private String lastVerifiedAt;
    private String createdAt;
    private String updatedAt;
    private String revokedAt;
    private String revokedReason;
}
