package com.example.feature.users.service;

import com.example.common.Clazzes;
import com.example.common.Departments;
import com.example.common.repository.LocalClazzRepository;
import com.example.common.repository.LocalDepartmentRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.users.dto.UserUpdateRequest;
import com.example.feature.users.event.UserDisabledEvent;
import com.example.feature.users.mapper.UserProfileMapper;
import com.example.feature.users.model.Users;
import com.example.feature.users.repository.UserRepository;
import com.example.service.BaseRedisService;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final BaseRedisService redisService;
    private final UserRepository userRepository;
    private final LocalClazzRepository localClazzRepository;
    private final LocalDepartmentRepository localDepartmentRepository;
    private final UserProfileMapper userMapper;
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
    public Users updateUserInfo(Long id, UserUpdateRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại trong hệ thống!"));

        userMapper.updateUserFromDto(request, user);

        if (request.getClassId() != null) {
            Clazzes clazz = localClazzRepository.findById(request.getClassId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,"Lớp học không tồn tại !"));
            user.setClazz(clazz);
        }

        if (request.getDepartmentId() != null) {
            Departments department = localDepartmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Trường hoặc Khoa không tồn tại !"));
            user.setDepartment(department);
        }

        return userRepository.save(user);
    }

    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }

    public Users getUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại trong hệ thống!"));
    }

    public Users getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại trong hệ thống!"));
    }

    @Transactional
    public void deleteUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String tokenKeycloakId = jwt.getClaimAsString("sub");
        boolean isOwner = false;

        if (user.getKeycloakId() != null && user.getKeycloakId().equals(tokenKeycloakId)) {
            isOwner = true;
        }

        if (isOwner) {
            user.setStatus(0); // Tự xóa
        } else {
            user.setStatus(2); // Admin xóa (Vô hiệu hóa)
        }

        // Logic chặn Keycloak & Redis
        if (user.getKeycloakId() != null) {
            // Chặn API ngay lập tức qua Redis
            redisService.set("BLOCKED_USER:" + user.getKeycloakId(), "BLOCKED", 7 * 24 * 60);

            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation userRepresentation = userResource.toRepresentation();

                userRepresentation.setEnabled(false); // Disable user
                userResource.update(userRepresentation);

                // (Optional) Logout các session hiện tại để kill token liền
                // userResource.logout();

            } catch (Exception e) {
                // Log lỗi bằng Slf4j, không dùng System.err
                log.error("Error disabling user in Keycloak for user {}: {}", user.getKeycloakId(), e.getMessage());
            }
        }

        userRepository.save(user);

        // --- GỬI SỰ KIỆN ĐÁ USER (Chỉ khi Admin xóa) ---
        if (!isOwner) {
            // Mapping dữ liệu khớp với UserDisabledEvent và NotificationRequest
            Long targetUserId = user.getId();

            eventPublisher.publishEvent(new UserDisabledEvent(
                    targetUserId,                                           // userId
                    "Tài khoản bị vô hiệu hóa",                             // title
                    "Tài khoản của bạn đã bị vô hiệu hóa bởi quản trị viên. Hệ thống sẽ đăng xuất ngay lập tức.", // message
                    99                                                      // type (Force Logout)
            ));
        }
    }

    @Transactional
    public void activateUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        user.setStatus(1); // 1 = Active

        if (user.getKeycloakId() != null) {
            redisService.delete("BLOCKED_USER:" + user.getKeycloakId());
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation userRepresentation = userResource.toRepresentation();

                userRepresentation.setEnabled(true);
                userResource.update(userRepresentation);

            } catch (Exception e) {
                System.err.println("Error activating user in Keycloak: " + e.getMessage());
            }
        }

        userRepository.save(user);
    }

//    private Long getID
}