package com.example.activityservice.feature.certificate_submissions.ai;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CertificateAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.ai-service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public CertificateAiClient(
            @Qualifier("aiRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public CertificateAiScanResult scan(
            byte[] imageBytes,
            String filename,
            String studentCode,
            String studentName) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Thiếu ảnh giấy khen để scan.");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(
                imageBytes,
                filename != null && !filename.isBlank() ? filename : "certificate.jpg"));
        if (studentCode != null && !studentCode.isBlank()) {
            body.add("student_code", studentCode);
        }
        if (studentName != null && !studentName.isBlank()) {
            body.add("student_name", studentName);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    aiServiceUrl + "/api/v1/certificates/scan",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            return mapResult(readAiPayload(response.getBody()));
        } catch (RestClientResponseException exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, resolveErrorMessage(
                    exception.getResponseBodyAsString(),
                    "AI service không thể scan giấy khen."));
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Không thể kết nối AI service để scan giấy khen.");
        }
    }

    private JsonNode readAiPayload(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service không trả về kết quả scan.");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if ("error".equalsIgnoreCase(text(root, "status"))) {
                throw new AppException(ErrorCode.INVALID_ACTION,
                        text(root, "message") != null ? text(root, "message") : "AI service không thể scan giấy khen.");
            }
            return root.has("data") ? root.get("data") : root;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service trả về dữ liệu giấy khen không hợp lệ.");
        }
    }

    private CertificateAiScanResult mapResult(JsonNode data) {
        JsonNode fields = data.path("fields");
        JsonNode suggestions = data.path("suggestions");
        return CertificateAiScanResult.builder()
                .rawText(text(data, "rawText", "raw_text", "text"))
                .extractedJson(toJson(data))
                .studentName(text(fields, "studentName", "student_name", "name"))
                .studentCode(text(fields, "studentCode", "student_code", "mssv"))
                .certificateTitle(text(fields, "certificateTitle", "awardTitle", "certificate_title", "award_title"))
                .issuer(text(fields, "issuer", "issuingUnit", "issuing_unit"))
                .issuedDate(text(fields, "issuedDate", "issued_date"))
                .achievement(text(fields, "achievement", "awardAchievement", "award_achievement"))
                .suggestedCategoryId(longValue(suggestions, "categoryId", "category_id"))
                .suggestedCategoryName(text(suggestions, "categoryName", "category_name"))
                .suggestedPoint(intValue(suggestions, "point", "suggestedPoint", "suggested_point"))
                .suggestionReason(text(suggestions, "reason", "suggestionReason", "suggestion_reason"))
                .confidence(decimal(data, "confidence", "aiConfidence", "ai_confidence"))
                .warnings(warnings(data.path("warnings")))
                .needsReview(booleanValue(data, "needsReview", "needs_review"))
                .build();
    }

    private String resolveErrorMessage(String responseBody, String fallback) {
        try {
            JsonNode payload = objectMapper.readTree(responseBody);
            return text(payload, "message") != null ? text(payload, "message") : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String text(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            if (node.hasNonNull(name)) {
                String value = node.get(name).asText();
                return value != null && !value.isBlank() ? value : null;
            }
        }
        return null;
    }

    private Long longValue(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            if (node.hasNonNull(name) && node.get(name).canConvertToLong()) {
                return node.get(name).asLong();
            }
        }
        return null;
    }

    private Integer intValue(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            if (node.hasNonNull(name) && node.get(name).canConvertToInt()) {
                return node.get(name).asInt();
            }
        }
        return null;
    }

    private Boolean booleanValue(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            if (node.hasNonNull(name)) {
                return node.get(name).asBoolean();
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isNumber()) {
                return BigDecimal.valueOf(value.asDouble());
            }
        }
        return null;
    }

    private List<String> warnings(JsonNode warningsNode) {
        List<String> warnings = new ArrayList<>();
        if (warningsNode == null || !warningsNode.isArray()) {
            return warnings;
        }
        warningsNode.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                warnings.add(item.asText());
            }
        });
        return warnings;
    }

    private String toJson(JsonNode data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
