package com.example.feature.users.controller;

import com.example.dto.ApiResponse;
import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.mapper.UserProfileMapper;
import com.example.feature.users.model.Users;
import com.example.feature.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileMapper userProfileMapper;

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
        // Service đã tự throw AppException nếu không tìm thấy -> GlobalHandler sẽ bắt
        Users user = userService.getUserById(id);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userProfileMapper.toUserResponse(user));

        return ResponseEntity.ok(apiResponse);
    }

    // 3. Update Profile
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {

        Users updatedUser = userService.updateUserInfo(id, request);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userProfileMapper.toUserResponse(updatedUser));
        apiResponse.setMessage("Cập nhật thông tin thành công");

        return ResponseEntity.ok(apiResponse);
    }

    // 4. Delete User
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Xóa người dùng thành công");

        return ResponseEntity.ok(apiResponse);
    }
}