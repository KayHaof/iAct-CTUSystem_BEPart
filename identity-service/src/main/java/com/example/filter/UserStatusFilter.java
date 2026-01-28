package com.example.filter;

import com.example.dto.ApiResponse;
import com.example.exception.ErrorCode;
import com.example.service.BaseRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatusFilter extends OncePerRequestFilter {

    private final BaseRedisService redisService;
    private final ObjectMapper objectMapper;
    private static final String REDIS_KEY_PREFIX = "BLOCKED_USER:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getClaimAsString("sub");
            String redisKey = REDIS_KEY_PREFIX + userId;

            if (redisService.hasKey(redisKey)) {
                log.warn("User {} bị chặn bởi Redis Blacklist", userId);
                sendErrorResponse(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setCode(ErrorCode.ACCOUNT_LOCKED.getCode());
        apiResponse.setMessage("Tài khoản của bạn đã bị khóa.");

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}