package com.example.feature.service.impl;

import com.example.dto.ApiResponse;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.service.NotificationUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class NotificationUserResolverImpl implements NotificationUserResolver {

    private static final ParameterizedTypeReference<ApiResponse<UserIdentityResponse>> USER_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient.Builder restClientBuilder;

    @Value("${app.services.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    @Override
    public Long resolveCurrentUserId(Jwt jwt) {
        if (jwt == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Authentication token is required");
        }

        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Token does not contain preferred_username");
        }

        UserIdentityResponse user = fetchUserData(username, jwt.getTokenValue());
        if (user == null || user.id() == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED, "User identity cannot be resolved");
        }
        return user.id();
    }

    private UserIdentityResponse fetchUserData(String username, String tokenValue) {
        String url = UriComponentsBuilder.fromUriString(userServiceUrl)
                .pathSegment("api", "v1", "users", "username", username)
                .build()
                .toUriString();

        try {
            ApiResponse<UserIdentityResponse> response = restClientBuilder.build()
                    .get()
                    .uri(url)
                    .headers(headers -> headers.setBearerAuth(tokenValue))
                    .retrieve()
                    .body(USER_RESPONSE_TYPE);

            return response == null ? null : response.getData();
        } catch (RestClientResponseException exception) {
            if (HttpStatus.NOT_FOUND.value() == exception.getStatusCode().value()
                    || HttpStatus.BAD_REQUEST.value() == exception.getStatusCode().value()) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED, "User not found for current token");
            }
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, "User Service cannot resolve current user");
        } catch (ResourceAccessException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, "User Service is unavailable");
        } catch (RestClientException exception) {
            throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE, "Cannot resolve current user from User Service");
        }
    }

    private record UserIdentityResponse(Long id) {
    }
}
