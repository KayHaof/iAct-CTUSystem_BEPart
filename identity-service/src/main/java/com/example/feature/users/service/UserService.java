package com.example.feature.users.service;

import com.example.common.Clazzes;
import com.example.common.Departments;
import com.example.common.repository.LocalClazzRepository;
import com.example.common.repository.LocalDepartmentRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.users.dto.ChangePasswordRequest;
import com.example.feature.users.dto.UserResponse;
import com.example.feature.users.dto.UserSyncDto;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.event.UserDisabledEvent;
import com.example.feature.users.mapper.UserProfileMapper;
import com.example.feature.users.model.Users;
import com.example.feature.users.repository.UserRepository;
import com.example.service.BaseRedisService;
import com.example.service.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final BaseRedisService redisService;
    private final UserRepository userRepository;
    private final LocalClazzRepository localClazzRepository;
    private final LocalDepartmentRepository localDepartmentRepository;
    private final UserProfileMapper userMapper;
    private final CloudinaryService cloudinaryService;
    private final Keycloak keycloak;
    private final String realm = "myRealm";
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void saveUserAfterRegistration(String keycloakId, String username, String email, Integer roleType) {
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USER_EXISTED, "Username này đã tồn tại");
        }
        Users user = Users.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .roleType(roleType)
                .status(1)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public UserResponse updateUserInfo(Long id, UserUpdateRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại!"));

        String oldAvatarUrl = user.getAvatarUrl();
        userMapper.updateUserFromDto(request, user);

        String newAvatarUrl = request.getAvatarUrl();
        if (newAvatarUrl != null && oldAvatarUrl != null && !oldAvatarUrl.equals(newAvatarUrl)) {
            deleteOldAvatar(oldAvatarUrl);
        }

        if (request.getClassId() != null) {
            Clazzes clazz = localClazzRepository.findById(request.getClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,"Lớp học không tồn tại !"));
            user.setClazz(clazz);
        }

        if (request.getDepartmentId() != null) {
            Departments department = localDepartmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khoa không tồn tại !"));
            user.setDepartment(department);
        }

        Users savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    public void changePasswordViaKeycloak(String bearerToken, ChangePasswordRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080" + "/realms/" + realm + "/account/credentials/password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", bearerToken);

        Map<String, String> body = new HashMap<>();
        body.put("currentPassword", request.getCurrentPassword());
        body.put("newPassword", request.getNewPassword());
        body.put("confirmation", request.getNewPassword());
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            log.error("Lỗi từ Keycloak: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Mật khẩu hiện tại không đúng hoặc mật khẩu mới chưa đạt yêu cầu!");
        }
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id){
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại!"));
        return userMapper.toResponse(user);
    }

    public UserResponse getUserByEmail(String email){
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy người dùng với email này!"));
        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String tokenKeycloakId = jwt.getClaimAsString("sub");
        boolean isOwner = user.getKeycloakId() != null && user.getKeycloakId().equals(tokenKeycloakId);

        if (isOwner) {
            user.setStatus(0);
        } else {
            user.setStatus(2);
        }

        if (user.getKeycloakId() != null) {
            redisService.set("BLOCKED_USER:" + user.getKeycloakId(), "BLOCKED", 7 * 24 * 60);
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setEnabled(false);
                userResource.update(userRepresentation);
            } catch (Exception e) {
                log.error("Lỗi khóa user trên Keycloak: {}", e.getMessage());
            }
        }

        userRepository.save(user);

        if (!isOwner) {
            eventPublisher.publishEvent(new UserDisabledEvent(
                    user.getId(),
                    "Tài khoản bị vô hiệu hóa",
                    "Tài khoản của bạn đã bị vô hiệu hóa bởi quản trị viên.",
                    99
            ));
        }
    }

    @Transactional
    public void activateUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(1);

        if (user.getKeycloakId() != null) {
            redisService.delete("BLOCKED_USER:" + user.getKeycloakId());
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setEnabled(true);
                userResource.update(userRepresentation);
            } catch (Exception e) {
                log.error("Lỗi mở khóa user trên Keycloak: {}", e.getMessage());
            }
        }
        userRepository.save(user);
    }

    public void deleteOldAvatar(String oldAvatarUrl) {
        cloudinaryService.deleteImageByUrl(oldAvatarUrl);
    }

    @Transactional
    public void syncUserFromKeycloak(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        if (!userRepository.existsByKeycloakId(keycloakId)) {
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            String fullName = jwt.getClaimAsString("name");

            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = (givenName != null ? givenName + " " : "") + (familyName != null ? familyName: "");
            }

            UserSyncDto syncDto = UserSyncDto.builder()
                    .keycloakId(keycloakId)
                    .username(jwt.getClaimAsString("preferred_username"))
                    .email(jwt.getClaimAsString("email"))
                    .fullName(fullName.trim())
                    .status(1)
                    .roleType(1)
                    .build();

            Users newUser = userMapper.toUserFromSyncDto(syncDto);
            userRepository.save(newUser);
            log.info("Đã đồng bộ user: {}", newUser.getUsername());
        }
    }
}