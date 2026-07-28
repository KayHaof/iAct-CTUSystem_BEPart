package com.example.activityservice.feature.face_embedding.ai;

import com.example.activityservice.feature.face_embedding.model.StudentFaceEmbeddingProjection;
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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
public class AiFaceVerificationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.ai-service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public AiFaceVerificationClient(
            @Qualifier("aiRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public FaceVerificationResult verify(
            StudentFaceEmbeddingProjection reference,
            byte[] liveImageBytes,
            String filename,
            Integer attempt,
            Integer maxAttempts) {
        if (liveImageBytes == null || liveImageBytes.length == 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Thieu anh khuon mat de xac thuc");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("live_file", new NamedByteArrayResource(liveImageBytes,
                filename != null && !filename.isBlank() ? filename : "attendance-face-live.jpg"));
        body.add("reference_vector", reference.getEmbeddingVector());
        body.add("attempt", String.valueOf(attempt != null ? attempt : 1));
        body.add("max_attempts", String.valueOf(maxAttempts != null ? maxAttempts : 5));
        body.add("strict_quality", "true");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    aiServiceUrl + "/api/v1/faces/verify",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            JsonNode payload = readAiPayload(response.getBody());
            return mapVerification(payload);
        } catch (RestClientResponseException exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, resolveErrorMessage(
                    exception.getResponseBodyAsString(),
                    "Khong the xac thuc khuon mat voi AI service"));
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Khong the ket noi AI service de xac thuc khuon mat");
        }
    }

    private JsonNode readAiPayload(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service khong tra ve ket qua xac thuc");
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service tra ve du lieu xac thuc khong hop le");
        }
    }

    private FaceVerificationResult mapVerification(JsonNode payload) {
        return FaceVerificationResult.builder()
                .verified(payload.path("verified").asBoolean(false))
                .decision(text(payload, "decision"))
                .allowRetry(booleanValue(payload, "allow_retry", "allowRetry"))
                .attempt(intValue(payload, "attempt"))
                .maxAttempts(intValue(payload, "max_attempts", "maxAttempts"))
                .remainingAttempts(intValue(payload, "remaining_attempts", "remainingAttempts"))
                .reasonCode(text(payload, "reason_code", "reasonCode"))
                .message(text(payload, "message"))
                .threshold(decimal(payload, "threshold"))
                .distance(decimal(payload, "distance"))
                .similarity(decimal(payload, "similarity"))
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
        for (String name : names) {
            if (node.hasNonNull(name)) {
                return node.get(name).asText();
            }
        }
        return null;
    }

    private Boolean booleanValue(JsonNode node, String... names) {
        for (String name : names) {
            if (node.hasNonNull(name)) {
                return node.get(name).asBoolean();
            }
        }
        return null;
    }

    private Integer intValue(JsonNode node, String... names) {
        for (String name : names) {
            if (node.hasNonNull(name) && node.get(name).canConvertToInt()) {
                return node.get(name).asInt();
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value != null && value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : null;
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
