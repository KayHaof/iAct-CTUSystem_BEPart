package com.example.userservice.feature.user_profile.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FaceEmbeddingExtractionResult {
    private String embeddingVector;
    private Integer vectorSize;
    private String modelName;
    private String detectorBackend;
    private String normalizationMethod;
    private BigDecimal qualityScore;
    private BigDecimal faceConfidence;
}
