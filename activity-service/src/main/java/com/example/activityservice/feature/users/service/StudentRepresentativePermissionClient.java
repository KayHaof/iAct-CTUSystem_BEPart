package com.example.activityservice.feature.users.service;

import com.example.activityservice.feature.users.dto.RepresentativeActivityPermissionResponse;
import com.example.dto.ApiResponse;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class StudentRepresentativePermissionClient {

    private static final ParameterizedTypeReference<ApiResponse<RepresentativeActivityPermissionResponse>>
            PERMISSION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;

    @Value("${app.services.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    public RepresentativeActivityPermissionResponse getCurrentStudentActivityPermission() {
        try {
            ResponseEntity<ApiResponse<RepresentativeActivityPermissionResponse>> response = restTemplate.exchange(
                    userServiceUrl + "/api/v1/class-representatives/me/activity-permission",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    PERMISSION_RESPONSE_TYPE);
            ApiResponse<RepresentativeActivityPermissionResponse> body = response.getBody();
            if (body == null || body.getData() == null) {
                throw new AppException(ErrorCode.FORBIDDEN,
                        "Khong xac dinh duoc quyen dai dien lop cua sinh vien.");
            }
            return body.getData();
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen dai dien lop de tao hoat dong.");
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        } catch (ResourceAccessException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, "User Service hien khong kha dung.");
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Khong the kiem tra quyen dai dien lop tu User Service.");
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            headers.setBearerAuth(jwt.getTokenValue());
        }
        return headers;
    }
}
