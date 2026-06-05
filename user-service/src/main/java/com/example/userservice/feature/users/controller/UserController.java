package com.example.userservice.feature.users.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.userservice.feature.user_profile.dto.UserUpdateRequest;
import com.example.userservice.feature.users.dto.UserResponse;
import com.example.userservice.feature.users.dto.ChangePasswordRequest;
import com.example.userservice.feature.users.dto.ImportResultDto;
import com.example.userservice.feature.users.service.UserImportService;
import com.example.userservice.feature.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserImportService userImportService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageDTO<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long classId) {
        return ApiResponse.success(userService.getUsers(page, size, keyword, roleType, departmentId, status, classId));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateUserProfile(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        userService.updateUserProfile(id, request);
        return ApiResponse.of(200, "Cap nhat thanh cong", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("Xoa hoac vo hieu nguoi dung thanh cong");
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> activeUser(@PathVariable long id) {
        userService.activateUser(id);
        return ApiResponse.success("Kich hoat tai khoan thanh cong");
    }

    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(userService.getMyInfo());
    }

    @PutMapping("/my-password")
    public ApiResponse<Void> changeMyPassword(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody ChangePasswordRequest request) {
        userService.changePasswordViaKeycloak(bearerToken, request);
        return ApiResponse.of(200, "Doi mat khau thanh cong!", null);
    }

    @PostMapping("/sync")
    public ApiResponse<String> syncUser(@AuthenticationPrincipal Jwt jwt) {
        userService.syncUserFromKeycloak(jwt);
        return ApiResponse.success("Dong bo nguoi dung thanh cong!");
    }

    @GetMapping("/search")
    public ApiResponse<UserResponse> getUserByEmail(@RequestParam("email") String email) {
        return ApiResponse.success(userService.getUserByEmail(email), "Tim thay nguoi dung thanh cong");
    }

    @GetMapping("/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Long>> getUserCounts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(userService.countUsersByRole(keyword));
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> resetPassword(@PathVariable Long id) {
        userService.sendResetPasswordEmail(id);
        return ApiResponse.success("Da gui email yeu cau dat lai mat khau thanh cong");
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResultDto> importUsersFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roleType") Integer roleType) {
        ImportResultDto result = userImportService.importUsers(file, roleType);
        return ApiResponse.success(result);
    }

    @GetMapping("/username/{username}")
    public ApiResponse<UserResponse> getUserByUsername(@PathVariable String username) {
        return ApiResponse.success(userService.getUserByUsername(username));
    }
}
