package com.example.feignClient;

import com.example.dto.ApiResponse;
import com.example.feature.registration.dto.UserSyncDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", path = "/api/v1/users")
public interface IdentityServiceClient {

    @GetMapping("/username/{username}")
    ApiResponse<UserSyncDto> getUserByUsername(@PathVariable String username);

}
