package com.example.feature.user_profile.controller;

import com.example.dto.ApiResponse;
import com.example.feature.user_profile.dto.CreateProfileDto;
import com.example.feature.user_profile.dto.ProfileDto;
import com.example.feature.user_profile.dto.UserUpdateRequest;
import com.example.feature.user_profile.repository.StudentProfileRepository;
import com.example.feature.user_profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;
    private final StudentProfileRepository studentProfileRepository;

    // 1. Tạo profile mới
    @PostMapping
    public ApiResponse<Void> createProfile(@RequestBody CreateProfileDto dto) {
        profileService.createProfile(dto);
        return ApiResponse.<Void>builder().message("Tạo profile thành công").build();
    }

    // 2. Lấy profile theo userId
    @GetMapping("/{userId}")
    public ApiResponse<ProfileDto> getProfileByUserId(@PathVariable Long userId) {
        ProfileDto result = profileService.getProfileByUserId(userId);
         System.out.println("===> DỮ LIỆU TỪ SERVICE TRẢ VỀ: " + result);

        return ApiResponse.<ProfileDto>builder()
                .result(result)
                .build();
    }

    // 3. Lấy nhiều profile cùng lúc
    @PostMapping("/batch")
    public ApiResponse<Map<Long, ProfileDto>> getProfilesBatch(@RequestBody List<Long> userIds) {
        return ApiResponse.<Map<Long, ProfileDto>>builder()
                .result(profileService.getProfilesBatch(userIds))
                .build();
    }

    // 4. Tìm kiếm ID theo điều kiện
    @GetMapping("/search-ids")
    public ApiResponse<List<Long>> searchUserIdsByCriteria(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Long classId)
    {
        return ApiResponse.<List<Long>>builder()
                .result(profileService.searchUserIds(keyword, departmentId, roleType, classId))
                .build();
    }

    // 5. Cập nhật profile
    @PutMapping("/{userId}")
    public ApiResponse<Void> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        profileService.updateUserProfile(userId, request);
        return ApiResponse.<Void>builder().message("Cập nhật thành công").build();
    }

    // 6. Import hàng loạt users
    @PostMapping("/batch-create")
    public ApiResponse<Void> createProfilesBatch(@RequestBody List<CreateProfileDto> profiles) {
        profileService.createProfilesBatch(profiles); // Xuống Service gọi studentProfileRepository.saveAll(...)
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
