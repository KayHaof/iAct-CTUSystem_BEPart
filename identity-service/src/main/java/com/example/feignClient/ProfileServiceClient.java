package com.example.feignClient;

import com.example.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
@FeignClient(name = "profile-service", url = "http://localhost:8080/profile", configuration = FeignClientInterceptor.class)
public interface ProfileServiceClient {

    @GetMapping("/api/v1/classes/department/{departmentId}/class-ids")
    ApiResponse<List<Long>> getClassIdsByDepartment(@PathVariable("departmentId") Long departmentId);
}