package com.example.activityservice.feature.certificate_submissions.ai;

import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CertificateImageFetcher {

    private final RestTemplate restTemplate;

    @Value("${app.certificate.allowed-image-hosts:res.cloudinary.com}")
    private String allowedImageHosts;

    @Value("${app.certificate.image-max-bytes:10485760}")
    private long maxBytes;

    public FetchedCertificateImage fetch(String imageUrl) {
        URI uri = validateUri(imageUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "iAct-activity-service/1.0");

        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Không thể tải ảnh giấy khen từ Cloudinary. Vui lòng thử lại.");
        }

        MediaType contentType = response.getHeaders().getContentType();
        if (!isAllowedImageType(contentType)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Link giấy khen phải trỏ tới file ảnh JPG, PNG hoặc WEBP.");
        }

        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ảnh giấy khen tải về đang trống.");
        }
        if (body.length > maxBytes) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ảnh giấy khen vượt quá dung lượng cho phép.");
        }

        return FetchedCertificateImage.builder()
                .bytes(body)
                .filename(resolveFilename(uri, contentType))
                .contentType(contentType != null ? contentType.toString() : null)
                .build();
    }

    private URI validateUri(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng cung cấp link ảnh giấy khen.");
        }

        URI uri;
        try {
            uri = new URI(imageUrl.trim());
        } catch (URISyntaxException exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Link ảnh giấy khen không hợp lệ.");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Link ảnh giấy khen phải dùng HTTPS.");
        }

        String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : null;
        if (host == null || !allowedHosts().contains(host)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ hỗ trợ ảnh giấy khen đã upload lên Cloudinary.");
        }

        if (uri.getUserInfo() != null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Link ảnh giấy khen không hợp lệ.");
        }

        return uri;
    }

    private Set<String> allowedHosts() {
        return Arrays.stream(allowedImageHosts.split(","))
                .map(host -> host.trim().toLowerCase(Locale.ROOT))
                .filter(host -> !host.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean isAllowedImageType(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        return MediaType.IMAGE_JPEG.isCompatibleWith(contentType)
                || MediaType.IMAGE_PNG.isCompatibleWith(contentType)
                || ("image".equalsIgnoreCase(contentType.getType())
                    && "webp".equalsIgnoreCase(contentType.getSubtype()));
    }

    private String resolveFilename(URI uri, MediaType contentType) {
        String path = uri.getPath();
        if (path != null && !path.isBlank()) {
            int slash = path.lastIndexOf('/');
            String candidate = slash >= 0 ? path.substring(slash + 1) : path;
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        if (contentType != null && MediaType.IMAGE_PNG.isCompatibleWith(contentType)) {
            return "certificate.png";
        }
        if (contentType != null && "webp".equalsIgnoreCase(contentType.getSubtype())) {
            return "certificate.webp";
        }
        return "certificate.jpg";
    }
}
