package com.example.activityservice.feature.certificate_submissions.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CertificateAiScanResult {
    private String rawText;
    private String extractedJson;
    private String studentName;
    private String studentCode;
    private String certificateTitle;
    private String issuer;
    private String issuedDate;
    private String achievement;
    private Long suggestedCategoryId;
    private String suggestedCategoryName;
    private Integer suggestedPoint;
    private String suggestionReason;
    private BigDecimal confidence;
    private List<String> warnings;
    private Boolean needsReview;
}
