package com.example.feature.users.controller;

import com.example.config.UserSecurity;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO; // 🚀 Import PageDTO
import com.example.feature.users.dto.ChangePasswordRequest;
import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSecurity userSecurity;

    // 1. Get All Users (Admin only)
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageDTO<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status) {

        return ApiResponse.<PageDTO<UserResponse>>builder()
                .result(userService.getUsers(page, size, keyword, roleType, departmentId, status))
                .build();
    }

    // 2. Get User By ID
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(id))
                .build();
    }

    // 3. Update Profile
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN') or @userSecurity.hasUserId(authentication, #id)")
    public ApiResponse<UserResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {

        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUserInfo(id, request))
                .message("Cập nhật thông tin thành công")
                .build();
    }

    // 4. Update - Del - Inactive status user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN') or @userSecurity.hasUserId(authentication, #id)")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.<String>builder()
                .message("Xóa hoặc vô hiệu người dùng thành công")
                .build();
    }

    // 5. Active user by admin
    @PutMapping("/{id}/active")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<String> activeUser(@PathVariable long id) {
        userService.activateUser(id);
        return ApiResponse.<String>builder()
                .message("Kích hoạt tài khoản thành công")
                .build();
    }

    // 6. Get info user by email login
    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String email = jwt.getClaimAsString("email");

        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserByEmail(email))
                .build();
    }

    // 7. Change password
    @PutMapping("/my-password")
    public ApiResponse<Void> changeMyPassword(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody ChangePasswordRequest request) {

        userService.changePasswordViaKeycloak(bearerToken, request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Đổi mật khẩu thành công!")
                .build();
    }

    // 8. Sync info user form keycloak
    @PostMapping("/sync")
    public ApiResponse<String> syncUser(@AuthenticationPrincipal Jwt jwt) {
        userService.syncUserFromKeycloak(jwt);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Đồng bộ người dùng thành công!")
                .build();
    }

    // 9. Search user by email
    @GetMapping("/search")
    public ApiResponse<UserResponse> getUserByEmail(@RequestParam("email") String email) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Tìm thấy người dùng thành công")
                .result(userService.getUserByEmail(email))
                .build();
    }

    // 10. Count user by role
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    @GetMapping("/counts")
    public ApiResponse<Map<String, Long>> getUserCounts(@RequestParam(required = false) String keyword) {
        return ApiResponse.<Map<String, Long>>builder()
                .result(userService.countUsersByRole(keyword))
                .build();
    }

    // 11. Reset password by admin
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('admin') or hasRole('ADMIN')")
    public ApiResponse<String> resetPassword(@PathVariable Long id) {
        userService.sendResetPasswordEmail(id);

        return ApiResponse.<String>builder()
                .message("Đã gửi email yêu cầu đặt lại mật khẩu thành công")
                .build();
    }
}