package com.example.feature.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface NotificationUserResolver {
    Long resolveCurrentUserId(Jwt jwt);
}
