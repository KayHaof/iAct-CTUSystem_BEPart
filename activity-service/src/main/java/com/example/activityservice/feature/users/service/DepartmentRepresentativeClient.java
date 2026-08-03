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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentRepresentativeClient {

    private static final ParameterizedTypeReference<ApiResponse<List<RepresentativeActivityPermissionResponse>>>
            REPRESENTATIVES_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;

    @Value("${app.services.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    public List<RepresentativeActivityPermissionResponse> getRepresentatives(
            Long departmentId,
            Long classId,
            Boolean active) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(userServiceUrl)
                    .path("/api/v1/class-representatives");
            if (departmentId != null) {
                builder.queryParam("departmentId", departmentId);
            }
            if (classId != null) {
                builder.queryParam("classId", classId);
            }
            if (active != null) {
                builder.queryParam("active", active);
            }

            ResponseEntity<ApiResponse<List<RepresentativeActivityPermissionResponse>>> response =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.GET,
                            new HttpEntity<>(createHeaders()),
                            REPRESENTATIVES_RESPONSE_TYPE);

            ApiResponse<List<RepresentativeActivityPermissionResponse>> body = response.getBody();
            return body != null && body.getData() != null ? body.getData() : List.of();
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Ban khong co quyen xem dai dien lop/chi doan cua don vi nay.");
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        } catch (ResourceAccessException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, "User Service hien khong kha dung.");
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Khong the lay danh sach dai dien lop/chi doan tu User Service.");
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
