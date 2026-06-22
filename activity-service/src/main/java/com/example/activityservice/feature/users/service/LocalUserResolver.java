package com.example.activityservice.feature.users.service;

import com.example.activityservice.feature.users.dto.UserSnapshot;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
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
public class LocalUserResolver {

    private static final ParameterizedTypeReference<ApiResponse<UserSnapshot>> USER_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final UserRepository userRepository;
    private final LocalUserProjectionService projectionService;
    private final RestTemplate restTemplate;

    @Value("${app.services.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    public Users resolveById(Long userId) {
        return userRepository.findById(userId)
                .orElseGet(() -> fetchAndProject("/api/v1/users/" + userId, "ID: " + userId));
    }

    public Users resolveByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> fetchAndProject(
                        "/api/v1/users/username/" + username,
                        "username: " + username));
    }

    private Users fetchAndProject(String path, String userReference) {
        try {
            ResponseEntity<ApiResponse<UserSnapshot>> response = restTemplate.exchange(
                    userServiceUrl + path,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    USER_RESPONSE_TYPE);
            ApiResponse<UserSnapshot> body = response.getBody();
            if (body == null || body.getData() == null) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED,
                        "Không tìm thấy User với " + userReference);
            }
            return projectionService.upsert(body.getData());
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound exception) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED,
                    "Không tìm thấy User với " + userReference);
        } catch (ResourceAccessException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "User Service hiện không khả dụng");
        } catch (AppException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                    "Không thể đồng bộ thông tin từ User Service");
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
