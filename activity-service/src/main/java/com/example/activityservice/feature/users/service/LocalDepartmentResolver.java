package com.example.activityservice.feature.users.service;

import com.example.activityservice.feature.departments.dto.DepartmentLookupResponse;
import com.example.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalDepartmentResolver {

    private static final ParameterizedTypeReference<ApiResponse<List<DepartmentLookupResponse>>> LOOKUP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${app.services.user-service-url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${app.cache.department-lookup-ttl-ms:1800000}")
    private long cacheTtlMs;

    public Optional<String> resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return Optional.empty();
        }
        String cachedName = getCachedName(departmentId);
        if (cachedName != null) {
            return Optional.of(cachedName);
        }
        return Optional.ofNullable(resolveDepartmentNames(List.of(departmentId)).get(departmentId));
    }

    public Map<Long, String> resolveDepartmentNames(Collection<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> ids = departmentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> result = new LinkedHashMap<>();
        Set<Long> missingIds = ids.stream()
                .filter(id -> {
                    String cachedName = getCachedName(id);
                    if (cachedName != null) {
                        result.put(id, cachedName);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());

        if (missingIds.isEmpty()) {
            return result;
        }

        fetchDepartments(missingIds).forEach(department -> {
            if (department.getId() != null && department.getName() != null && !department.getName().isBlank()) {
                cache.put(department.getId(), new CacheEntry(department.getName(), System.currentTimeMillis()));
                result.put(department.getId(), department.getName());
            }
        });
        return result;
    }

    private List<DepartmentLookupResponse> fetchDepartments(Set<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return List.of();
        }

        String url = UriComponentsBuilder.fromUriString(userServiceUrl)
                .path("/api/v1/departments/public-lookup")
                .queryParam("ids", departmentIds)
                .toUriString();

        try {
            ResponseEntity<ApiResponse<List<DepartmentLookupResponse>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    LOOKUP_RESPONSE_TYPE);
            ApiResponse<List<DepartmentLookupResponse>> body = response.getBody();
            return body != null && body.getData() != null ? body.getData() : List.of();
        } catch (RestClientException exception) {
            log.warn("Khong the lay department lookup tu user-service: {}", exception.getMessage());
            return Collections.emptyList();
        }
    }

    private String getCachedName(Long departmentId) {
        CacheEntry entry = cache.get(departmentId);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.cachedAt() > cacheTtlMs) {
            cache.remove(departmentId);
            return null;
        }
        return entry.name();
    }

    private record CacheEntry(String name, long cachedAt) {
    }
}
