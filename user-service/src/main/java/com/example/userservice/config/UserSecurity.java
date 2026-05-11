package com.example.userservice.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component("userSecurity")
public class UserSecurity {
    public boolean isOwnerOrAdmin(Authentication authentication, String targetKeycloakId) {
        if (authentication == null || authentication.getName() == null) {
            return false;
        }

        // 1. Check Admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        // 2. Check Owner
        String currentKeycloakId = authentication.getName();
        return currentKeycloakId.equals(targetKeycloakId);
    }
}