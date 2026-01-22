package com.example.feature.auth.controller;

import com.example.feature.auth.dto.LoginRequest;
import com.example.feature.auth.dto.RegisterRequest;
import com.example.feature.auth.service.AuthService;
import com.example.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Object tokenResponse = authService.loginUser(request);
        return ResponseEntity.ok(tokenResponse);
    }


    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaim("preferred_username");

        return userRepository.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}