package com.example.userservice.config;

import com.example.dto.ApiResponse;
import com.example.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.*;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .securityMatcher("/auth/**", "/error", "/actuator/**", "/api/v1/departments/count",
                        "/api/v1/departments/public-lookup", "/api/v1/majors/count")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/departments/count").permitAll()
                        .requestMatchers("/api/v1/departments/public-lookup").permitAll()
                        .requestMatchers("/api/v1/majors/count").permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain protectedFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .securityMatcher("/api/v1/**", "/api/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .accessDeniedHandler(accessDeniedHandler()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Authentication error: {}", authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json");
                            ApiResponse<Object> apiResponse = new ApiResponse<>();
                            apiResponse.setCode(ErrorCode.UNAUTHENTICATED.getCode());
                            apiResponse.setMessage("User Service: Token không hợp lệ hoặc hết hạn");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
                        }));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            if (roles == null) {
                return Collections.emptyList();
            }

            return new ArrayList<>(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .toList());
        });

        return jwtConverter;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access denied: {}", accessDeniedException.getMessage());
            response.setStatus(403);
            response.setContentType("application/json");
            ApiResponse<Object> apiResponse = new ApiResponse<>();
            apiResponse.setCode(ErrorCode.FORBIDDEN.getCode());
                            apiResponse.setMessage("User Service: Bạn không có quyền truy cập tài nguyên này");
            response.getWriter().write(new ObjectMapper().writeValueAsString(apiResponse));
        };
    }
}
