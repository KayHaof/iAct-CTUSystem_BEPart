package com.example.activityservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigTest {

    @Test
    void usesPreferredUsernameInsteadOfKeycloakSubjectAsPrincipal() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "8b20bf20-4be2-42c2-bbaa-53a1e90b9b76",
                        "preferred_username", "dept_cict",
                        "realm_access", Map.of("roles", java.util.List.of("DEPARTMENT"))));

        AbstractAuthenticationToken authentication =
                new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertEquals("dept_cict", authentication.getName());
    }
}
