package com.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistCheckFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> Mono.justOrEmpty(securityContext.getAuthentication()))
                .flatMap(authentication -> {
                    if (authentication.getPrincipal() instanceof Jwt jwt) {
                        String keycloakId = jwt.getClaimAsString("sub");
                        String redisKey = "BLOCKED_USER:" + keycloakId;

                        return reactiveRedisTemplate.hasKey(redisKey)
                                .flatMap(isBlocked -> {
                                    if (isBlocked) {
                                        log.warn("Chặn đứng request từ user bị khóa: {}", keycloakId);
                                        return Mono.error(new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "Tài khoản của bạn đã bị khóa!"
                                        ));
                                    }
                                    return chain.filter(exchange);
                                });
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}