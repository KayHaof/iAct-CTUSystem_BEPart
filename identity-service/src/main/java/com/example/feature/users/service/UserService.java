package com.example.feature.users.service;

import com.example.common.Clazzes;
import com.example.common.repository.LocalClazzRepository;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
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
import com.example.feignClient.ProfileServiceClient;
import com.example.service.BaseRedisService;
import com.example.service.CloudinaryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Collections;
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
    private final UserProfileMapper userMapper;
    private final CloudinaryService cloudinaryService;
    private final Keycloak keycloak;
    private final String realm = "myRealm";
    private final ApplicationEventPublisher eventPublisher;
    private final ProfileServiceClient profileServiceClient;

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
            throw new AppException(ErrorCode.INCORRECT_VALUE, "Mật khẩu hiện tại không đúng hoặc mật khẩu mới chưa đạt yêu cầu!");
        }
    }

    // Reset password (action of admin)
    @Transactional
    public void sendResetPasswordEmail(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại!"));

        if (user.getKeycloakId() == null) {
            throw new RuntimeException("Tài khoản này chưa được đồng bộ với Keycloak!");
        }

        try {
            keycloak.realm(realm)
                    .users()
                    .get(user.getKeycloakId())
                    .executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));

            log.info("Đã yêu cầu Keycloak gửi email reset password cho user: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Lỗi khi gửi email reset mật khẩu qua Keycloak: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_ACTION, "Lỗi hệ thống khi gửi email đặt lại mật khẩu! Vui lòng kiểm tra lại cấu hình SMTP.");
        }
    }

    public PageDTO<UserResponse> getUsers(int page, int size, String keyword, Integer roleType, Long departmentId, Integer status) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by("id").descending());

        Specification<Users> spec = (root, query, cb) -> {
            jakarta.persistence.criteria.Predicate predicate = cb.conjunction();

            // 1. Lọc Keyword
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("fullName")), likeKeyword),
                        cb.like(cb.lower(root.get("email")), likeKeyword),
                        cb.like(cb.lower(root.get("studentCode")), likeKeyword)
                ));
            }

            // 2. Lọc Role
            if (roleType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("roleType"), roleType));
            }

            // 3. Lọc Trạng thái
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            //  4. LỌC THEO KHOA BẰNG FEIGN CLIENT (MICROSERVICES WAY)
            if (departmentId != null) {
                try {
                    // Gọi sang Profile-Service lấy danh sách Class ID
                    ApiResponse<List<Long>> response = profileServiceClient.getClassIdsByDepartment(departmentId);
                    List<Long> classIds = response.getResult();

                    if (classIds != null && !classIds.isEmpty()) {
                        // Nếu Khoa có lớp -> Lọc những User có clazz.id nằm trong danh sách này
                        predicate = cb.and(predicate, root.get("clazz").get("id").in(classIds));
                    } else {
                        // Nếu Khoa này KHÔNG có lớp nào -> Chắc chắn không có sinh viên
                        // Dùng cb.disjunction() để bắt buộc trả về danh sách rỗng (1 = 0)
                        predicate = cb.and(predicate, cb.disjunction());
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi gọi profile-service lấy danh sách class_id: {}", e.getMessage());
                    predicate = cb.and(predicate, cb.disjunction());
                }
            }

            return predicate;
        };

        Page<Users> usersPage = userRepository.findAll(spec, pageable);

        List<UserResponse> data = usersPage.getContent().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());

        return new PageDTO<>(usersPage, data);
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
                fullName = (familyName != null ? familyName: "") + (givenName != null ? givenName + " " : "");
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

    public Map<String, Long> countUsersByRole(String keyword) {
        Specification<Users> baseSpec = (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), likeKeyword),
                    cb.like(cb.lower(root.get("email")), likeKeyword),
                    cb.like(cb.lower(root.get("studentCode")), likeKeyword)
            );
        };

        // Đếm cho từng Role sử dụng baseSpec kết hợp với điều kiện Role
        long studentCount = userRepository.count(baseSpec.and((root, query, cb) -> cb.equal(root.get("roleType"), 1)));
        long facultyCount = userRepository.count(baseSpec.and((root, query, cb) -> cb.equal(root.get("roleType"), 2)));
        long adminCount = userRepository.count(baseSpec.and((root, query, cb) -> cb.equal(root.get("roleType"), 3)));

        Map<String, Long> counts = new HashMap<>();
        counts.put("student", studentCount);
        counts.put("faculty", facultyCount);
        counts.put("admin", adminCount);

        return counts;
    }
}