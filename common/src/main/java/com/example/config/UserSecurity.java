package com.example.config;

import com.example.repository.CommonUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("userSecurity")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.datasource.url")
public class UserSecurity {

    private final CommonUserRepository userRepository;

    public boolean hasUserId(Authentication authentication, Long userId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        return userRepository.findById(userId)
                .map(user -> {
                    System.out.println(">> Check username: " + user.getUsername());
                    System.out.println(">> Check keycloakID: " + user.getKeycloakId());
                    String currentUsername = authentication.getName();

                    System.out.println(">> Check current username: " + currentUsername);

                    return user.getKeycloakId().equals(currentUsername);
                })
                .orElse(false);
    }
}