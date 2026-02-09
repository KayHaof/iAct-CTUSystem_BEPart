package com.example.feature.users.controller;

import com.example.config.UserSecurity;
import com.example.dto.ApiResponse;
import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.mapper.UserProfileMapper;
import com.example.feature.users.model.Users;
import com.example.feature.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileMapper userProfileMapper;
    private  final UserSecurity userSecurity;

    // 1. Get All Users (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<Users> users = userService.getAllUsers();

        List<UserResponse> userResponses = users.stream()
                .map(userProfileMapper::toUserResponse)
                .collect(Collectors.toList());

        ApiResponse<List<UserResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userResponses);

        return ResponseEntity.ok(apiResponse);
    }

    // 2. Get User By ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        Users user = userService.getUserById(id);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userProfileMapper.toUserResponse(user));

        return ResponseEntity.ok(apiResponse);
    }

    // 3. Update Profile
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.hasUserId(authentication, #id)")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {

        Users updatedUser = userService.updateUserInfo(id, request);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userProfileMapper.toUserResponse(updatedUser));
        apiResponse.setMessage("Cập nhật thông tin thành công");

        return ResponseEntity.ok(apiResponse);
    }

    // 4. Update - Del - Inactive status user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.hasUserId(authentication, #id)")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Xóa hoặc vô hiệu người dùng thành công");

        return ResponseEntity.ok(apiResponse);
    }

    // 5. Active user by admin
    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> activeUser(@PathVariable long id){
        userService.activateUser(id);

        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Kích hoạt tài khoản thành công");

        return ResponseEntity.ok(apiResponse);
    }

    // 6. Get info user by email
    @GetMapping("/my-info")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        // Ép kiểu Principal về Jwt
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Lấy chính xác claim "email" (hoặc "preferred_username" tùy config Keycloak)
        String email = jwt.getClaimAsString("email");

        // Log ra kiểm tra thử
        System.out.println(">> Email extracted: " + email);

        Users user = userService.getUserByEmail(email);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userProfileMapper.toUserResponse(user));

        return ResponseEntity.ok(apiResponse);
    }
}