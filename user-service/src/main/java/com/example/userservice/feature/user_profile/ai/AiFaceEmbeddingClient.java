package com.example.userservice.feature.user_profile.ai;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AiFaceEmbeddingClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.services.ai-service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public FaceEmbeddingExtractionResult extractFromImageUrl(String imageUrl) {
        byte[] imageBytes = downloadImage(imageUrl);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(imageBytes, "student-face-reference.jpg"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    aiServiceUrl + "/api/v1/faces/extract?strict_quality=true",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            JsonNode payload = readAiPayload(response.getBody());
            if (payload == null || !"success".equalsIgnoreCase(payload.path("status").asText())) {
                throw new AppException(ErrorCode.INVALID_ACTION, resolveMessage(payload,
                        "Không thể trích xuất vector khuôn mặt từ ảnh đăng ký"));
            }
            return mapExtraction(payload);
        } catch (RestClientResponseException exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, resolveErrorMessage(
                    exception.getResponseBodyAsString(),
                    "Ảnh đăng ký không hợp lệ hoặc AI service không thể xử lý"));
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Không thể kết nối AI service để trích xuất vector khuôn mặt");
        }
    }

    private JsonNode readAiPayload(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service không trả về dữ liệu");
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service trả về dữ liệu không hợp lệ");
        }
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Không tải được ảnh từ Cloudinary");
            }
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType != null && !MediaType.IMAGE_JPEG.includes(contentType)
                    && !MediaType.IMAGE_PNG.includes(contentType)
                    && !MediaType.IMAGE_GIF.includes(contentType)
                    && !MediaType.APPLICATION_OCTET_STREAM.includes(contentType)) {
                throw new AppException(ErrorCode.INVALID_ACTION, "URL Cloudinary không phải là ảnh hợp lệ");
            }
            return body;
        } catch (RestClientResponseException exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không tải được ảnh từ Cloudinary");
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, "Không thể tải ảnh từ Cloudinary");
        }
    }

    private FaceEmbeddingExtractionResult mapExtraction(JsonNode payload) {
        JsonNode vector = payload.get("vector");
        if (vector == null || !vector.isArray() || vector.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "AI service không trả về vector khuôn mặt");
        }
        try {
            return FaceEmbeddingExtractionResult.builder()
                    .embeddingVector(objectMapper.writeValueAsString(vector))
                    .vectorSize(payload.path("vector_size").isNumber()
                            ? payload.path("vector_size").asInt()
                            : vector.size())
                    .modelName(payload.path("model").asText(null))
                    .detectorBackend(payload.path("detector_backend").asText(null))
                    .normalizationMethod(payload.path("normalization").asText(null))
                    .qualityScore(decimal(payload.path("quality").path("score")))
                    .faceConfidence(decimal(payload.path("face").path("confidence")))
                    .build();
        } catch (Exception exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể đọc kết quả vector từ AI service");
        }
    }

    private BigDecimal decimal(JsonNode node) {
        return node != null && node.isNumber() ? BigDecimal.valueOf(node.asDouble()) : null;
    }

    private String resolveErrorMessage(String responseBody, String fallback) {
        try {
            return resolveMessage(objectMapper.readTree(responseBody), fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String resolveMessage(JsonNode payload, String fallback) {
        if (payload != null && payload.hasNonNull("message")) {
            return payload.get("message").asText();
        }
        return fallback;
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
