package com.example.userservice.feature.user_profile.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentFaceEmbeddingRequest {
    @NotBlank(message = "Thieu URL anh goc")
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
}
