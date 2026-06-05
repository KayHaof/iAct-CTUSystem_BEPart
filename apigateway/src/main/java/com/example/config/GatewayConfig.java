package com.example.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r
                        .path("/user/api/v1/**")
                        .filters(f -> f.rewritePath("/user/(?<segment>.*)", "/${segment}"))
                        .uri("lb://user-service"))
                .route("activity-service-route", r -> r
                        .path("/activity/api/v1/**")
                        .filters(f -> f.rewritePath("/activity/(?<segment>.*)", "/${segment}"))
                        .uri("lb://activity-service"))
                .route("notification-service-route", r -> r
                        .path("/notification/api/v1/**")
                        .filters(f -> f.rewritePath("/notification/(?<segment>.*)", "/${segment}"))
                        .uri("lb://notification-service"))
                .build();
    }
}
