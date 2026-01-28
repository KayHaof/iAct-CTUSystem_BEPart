package com.example.config;

import com.example.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean hasUserId(Authentication authentication, Long userId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

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