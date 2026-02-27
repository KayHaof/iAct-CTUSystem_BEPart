package com.example.feature.users.controller;

import com.example.config.UserSecurity;
import com.example.dto.ApiResponse;
import com.example.feature.users.dto.ChangePasswordRequest;
import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSecurity userSecurity;

    // 1. Get All Users (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> userResponses = userService.getAllUsers();

        ApiResponse<List<UserResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userResponses);

        return ResponseEntity.ok(apiResponse);
    }

    // 2. Get User By ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse userResponse = userService.getUserById(id);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userResponse);

        return ResponseEntity.ok(apiResponse);
    }

    // 3. Update Profile
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.hasUserId(authentication, #id)")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {

        UserResponse updatedUser = userService.updateUserInfo(id, request);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(updatedUser);
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

    // 6. Get info user by email login
    @GetMapping("/my-info")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        // Ép kiểu Principal về Jwt
        Jwt jwt = (Jwt) authentication.getPrincipal();

        // Lấy chính xác claim "email"
        String email = jwt.getClaimAsString("email");

        UserResponse userResponse = userService.getUserByEmail(email);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userResponse);

        return ResponseEntity.ok(apiResponse);
    }

    // 7. Change password
    @PutMapping("/my-password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody ChangePasswordRequest request) {

        userService.changePasswordViaKeycloak(bearerToken, request);

        return ResponseEntity.ok(new ApiResponse<>(200, "Đổi mật khẩu thành công!", null));
    }

    // 8. Sync info user form keycloak
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncUser(@AuthenticationPrincipal Jwt jwt) {
        userService.syncUserFromKeycloak(jwt);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("Đồng bộ người dùng thành công!");

        return ResponseEntity.ok(response);
    }

    // 9. Search user by email
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@RequestParam("email") String email) {
        UserResponse userResponse = userService.getUserByEmail(email);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setCode(200);
        apiResponse.setMessage("Tìm thấy người dùng thành công");
        apiResponse.setResult(userResponse);

        return ResponseEntity.ok(apiResponse);
    }
}