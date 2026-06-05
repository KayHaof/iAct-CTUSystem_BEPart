package com.example.userservice.feature.user_profile.controller;

import com.example.dto.ApiResponse;
import com.example.userservice.feature.user_profile.dto.CreateProfileDto;
import com.example.userservice.feature.user_profile.dto.ProfileDto;
import com.example.userservice.feature.user_profile.dto.UserUpdateRequest;
import com.example.userservice.feature.user_profile.repository.StudentProfileRepository;
import com.example.userservice.feature.user_profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;
    private final StudentProfileRepository studentProfileRepository;

    @PostMapping
    public ApiResponse<Void> createProfile(@RequestBody CreateProfileDto dto) {
        profileService.createProfile(dto);
        return ApiResponse.of(201, "Tao profile thanh cong", null);
    }

    @GetMapping("/{userId}")
    public ApiResponse<ProfileDto> getProfileByUserId(@PathVariable Long userId) {
        ProfileDto result = profileService.getProfileByUserId(userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/batch")
    public ApiResponse<Map<Long, ProfileDto>> getProfilesBatch(@RequestBody List<Long> userIds) {
        return ApiResponse.success(profileService.getProfilesBatch(userIds));
    }

    @GetMapping("/search-ids")
    public ApiResponse<Set<Long>> searchUserIdsByCriteria(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Long classId) {
        return ApiResponse.success(profileService.searchUserIds(keyword, departmentId, roleType, classId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<Void> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        profileService.updateUserProfile(userId, request);
        return ApiResponse.of(200, "Cap nhat thanh cong", null);
    }

    @PostMapping("/batch-create")
    public ApiResponse<Void> createProfilesBatch(@RequestBody List<CreateProfileDto> profiles) {
        profileService.createProfilesBatch(profiles);
        return ApiResponse.success(null);
    }

    @PostMapping("/check-student-codes")
    public ApiResponse<Set<String>> checkExistingStudentCodes(@RequestBody Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return ApiResponse.success(new HashSet<>());
        }
        return ApiResponse.success(studentProfileRepository.findExistingStudentCodes(codes));
    }
}
