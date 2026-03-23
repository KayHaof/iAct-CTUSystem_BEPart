package com.example.feignClient;

import com.example.dto.ApiResponse;
import com.example.feature.users.dto.CreateProfileDto;
import com.example.feature.users.dto.ProfileDto;
import com.example.feature.users.dto.UserUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(name = "profile-service", url = "http://localhost:8080/profile", configuration = FeignClientInterceptor.class)
public interface ProfileServiceClient {

    // 1. Lấy chi tiết 1 hồ sơ
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<ProfileDto> getProfileByUserId(@PathVariable("userId") Long userId);

    // 2. Lấy một nhóm hồ sơ
    @PostMapping("/api/v1/users/batch")
    ApiResponse<Map<Long, ProfileDto>> getProfilesBatch(@RequestBody List<Long> userIds);

    // 3. Tìm danh sách User ID dựa trên keyword và departmentId
    @GetMapping("/api/v1/users/search-ids")
    ApiResponse<List<Long>> searchUserIdsByCriteria(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "classId", required = false) Long classId
    );

    // 4. Cập nhật hồ sơ (Phân lớp, đổi tên, đổi avatar...)
    @PutMapping("/api/v1/users/{userId}")
    ApiResponse<Void> updateUserProfile(
            @PathVariable("userId") Long userId,
            @RequestBody UserUpdateRequest request
    );

    // 5. Tạo hồ sơ mặc định lúc mới đồng bộ từ Keycloak
    @PostMapping("/api/v1/users")
    ApiResponse<Void> createProfile(@RequestBody CreateProfileDto profileDto);

    // 6. BỔ SUNG: Tạo hồ sơ HÀNG LOẠT (Dùng cho tính năng Import Excel)
    @PostMapping("/api/v1/users/batch-create")
    ApiResponse<Void> createProfilesBatch(@RequestBody List<CreateProfileDto> profiles);

    @PostMapping("/api/v1/users/check-student-codes")
    ApiResponse<Set<String>> checkExistingStudentCodes(@RequestBody Set<String> codes);
}