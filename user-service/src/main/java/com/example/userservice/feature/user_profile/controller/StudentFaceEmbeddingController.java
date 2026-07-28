package com.example.userservice.feature.user_profile.controller;

import com.example.dto.ApiResponse;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.userservice.feature.user_profile.dto.StudentFaceEmbeddingRequest;
import com.example.userservice.feature.user_profile.dto.StudentFaceEmbeddingResponse;
import com.example.userservice.feature.user_profile.service.StudentFaceEmbeddingService;
import com.example.userservice.feature.users.model.Users;
import com.example.userservice.feature.users.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class StudentFaceEmbeddingController {

    private final StudentFaceEmbeddingService studentFaceEmbeddingService;
    private final UserRepository userRepository;

    @PutMapping("/me/face-embedding")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentFaceEmbeddingResponse> upsertMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid StudentFaceEmbeddingRequest request) {
        return ApiResponse.success(
                studentFaceEmbeddingService.upsert(resolveCurrentUserId(jwt), request),
                "Luu vector khuon mat sinh vien thanh cong");
    }

    @GetMapping("/me/face-embedding/active")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentFaceEmbeddingResponse> getMine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(studentFaceEmbeddingService.getActive(resolveCurrentUserId(jwt)));
    }

    @PatchMapping("/me/face-embedding/revoke")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentFaceEmbeddingResponse> revokeMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(
                studentFaceEmbeddingService.revoke(resolveCurrentUserId(jwt), reason),
                "Thu hoi vector khuon mat sinh vien thanh cong");
    }

    @PutMapping("/{userId}/face-embedding")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentFaceEmbeddingResponse> upsert(
            @PathVariable Long userId,
            @RequestBody @Valid StudentFaceEmbeddingRequest request) {
        return ApiResponse.success(
                studentFaceEmbeddingService.upsert(userId, request),
                "Luu vector khuon mat sinh vien thanh cong");
    }

    @GetMapping("/{userId}/face-embedding/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentFaceEmbeddingResponse> getActive(@PathVariable Long userId) {
        return ApiResponse.success(studentFaceEmbeddingService.getActive(userId));
    }

    @PatchMapping("/{userId}/face-embedding/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentFaceEmbeddingResponse> revoke(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        return ApiResponse.success(
                studentFaceEmbeddingService.revoke(userId, reason),
                "Thu hoi vector khuon mat sinh vien thanh cong");
    }

    @PostMapping("/face-embeddings/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> replay() {
        return ApiResponse.success(
                studentFaceEmbeddingService.replayAll(),
                "Da len lich dong bo lai vector khuon mat sinh vien");
    }

    private Long resolveCurrentUserId(Jwt jwt) {
        if (jwt == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Khong tim thay thong tin dang nhap");
        }
        String username = jwt.getClaimAsString("preferred_username");
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Khong tim thay tai khoan dang nhap"));
        return user.getId();
    }
}
