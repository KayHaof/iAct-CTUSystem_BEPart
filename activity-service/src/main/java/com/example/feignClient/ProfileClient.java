package com.example.feignClient;

import com.example.common.dto.ProfileDto;
import com.example.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "profile-service", path = "/api/v1/users")
public interface ProfileClient {

    @GetMapping("/{userId}")
    ApiResponse<ProfileDto> getProfile(@PathVariable("userId") Long userId);

    @PostMapping("/batch")
    ApiResponse<Map<Long, ProfileDto>> getProfilesBatch(@RequestBody List<Long> userIds);

    @GetMapping("/search-ids")
    ApiResponse<List<Long>> searchUserIdsByCriteria(@RequestParam(value = "keyword", required = false) String keyword);
}