package com.example.initialProject.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:3000",
                        "http://localhost:4200"));

        // Các method được phép
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Cho phép tất cả header
        config.setAllowedHeaders(List.of("*"));

        // Hiển thị các header này cho FE (vd: Authorization, Set-Cookie)
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        config.setAllowCredentials(true);

        // Cache preflight 1 giờ
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
