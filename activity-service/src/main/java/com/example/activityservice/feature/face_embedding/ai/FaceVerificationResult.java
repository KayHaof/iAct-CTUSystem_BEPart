package com.example.activityservice.feature.face_embedding.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FaceVerificationResult {
    private Boolean verified;
    private String decision;
    private Boolean allowRetry;
    private Integer attempt;
    private Integer maxAttempts;
    private Integer remainingAttempts;
    private String reasonCode;
    private String message;
    private BigDecimal threshold;
    private BigDecimal distance;
    private BigDecimal similarity;
}
