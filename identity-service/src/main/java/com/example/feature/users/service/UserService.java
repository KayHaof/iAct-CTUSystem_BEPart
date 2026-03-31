package com.example.feature.users.service;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.users.dto.*;
import com.example.feature.users.event.UserDisabledEvent;
import com.example.feature.users.mapper.UserProfileMapper;
import com.example.feature.users.model.Users;
import com.example.feature.users.repository.UserRepository;
import com.example.feignClient.ProfileServiceClient;
import com.example.service.BaseRedisService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
    private final UserProfileMapper userMapper;
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

    public UserResponse getMyInfo() {
        // 1. Lấy thông tin Keycloak ID từ token hiện tại
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getClaimAsString("sub");

        // 2. Lấy Core User từ DB
        Users user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy thông tin xác thực cục bộ!"));

        // 3. Gọi FeignClient sang Profile Service để lấy dữ liệu Hồ sơ (Avatar, FullName, Class...)
        ProfileDto profile = null;
        try {
            profile = profileServiceClient.getProfileByUserId(user.getId()).getResult();
        } catch (Exception e) {
            log.warn("Không thể lấy profile cho userId {}: {}", user.getId(), e.getMessage());
        }

        // 4. Trộn 2 cục data lại và trả về cho Frontend
        return userMapper.toResponseAggregated(user, profile);
    }

    @Transactional
    public UserResponse updateUserInfo(Long id, UserUpdateRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại!"));

        // 1. Gọi Feign Client
        try {
            profileServiceClient.updateUserProfile(id, request);
        } catch (Exception e) {
            log.error("Lỗi khi update profile thông qua FeignClient: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể cập nhật hồ sơ lúc này!");
        }

        userRepository.touchUpdatedAt(id);
        return getUserById(id);
    }

    public void changePasswordViaKeycloak(String bearerToken, ChangePasswordRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/realms/" + realm + "/account/credentials/password";

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

    @Transactional
    public void sendResetPasswordEmail(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người dùng không tồn tại!"));

        if (user.getKeycloakId() == null) throw new RuntimeException("Tài khoản này chưa được đồng bộ với Keycloak!");

        try {
            keycloak.realm(realm).users().get(user.getKeycloakId()).executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
            log.info("Đã yêu cầu Keycloak gửi email reset password cho user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email reset mật khẩu: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_ACTION, "Lỗi cấu hình SMTP.");
        }
    }

    // Lấy danh sách người dùng
    public PageDTO<UserResponse> getUsers(int page, int size, String keyword, Integer roleType, Long departmentId, Integer status, Long classId) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by("id").descending());
        List<Long> matchingUserIds = null;

        if ((keyword != null && !keyword.trim().isEmpty()) || departmentId != null || classId != null) {
            try {
                ApiResponse<List<Long>> response = profileServiceClient.searchUserIdsByCriteria(keyword, departmentId, classId);
                matchingUserIds = response.getResult();

                if (matchingUserIds == null || matchingUserIds.isEmpty()) {
                    return new PageDTO<>(Page.empty(), Collections.emptyList());
                }
            } catch (Exception e) {
                log.error("Lỗi khi gọi Profile-Service để search: {}", e.getMessage());
                return new PageDTO<>(Page.empty(), Collections.emptyList());
            }
        }
        // 2. Build Specification
        Specification<Users> spec = getUsersSpecification(roleType, status, matchingUserIds);

        // 3. Query Local DB lấy danh sách tài khoản
        Page<Users> usersPage = userRepository.findAll(spec, pageable);
        List<Long> pageUserIds = usersPage.getContent().stream().map(Users::getId).toList();

        if (pageUserIds.isEmpty()) return new PageDTO<>(usersPage, Collections.emptyList());

        // 4. Batch Call qua Profile Service lấy Profile của những tài khoản này
        Map<Long, ProfileDto> profileMap = new HashMap<>();
        try {
            ApiResponse<Map<Long, ProfileDto>> profileRes = profileServiceClient.getProfilesBatch(pageUserIds);
            if (profileRes.getResult() != null) {
                profileMap = profileRes.getResult();
            }
        } catch (Exception e) {
            log.warn("Không thể lấy profile batch từ Profile-Service: {}", e.getMessage());
        }

        // 5. Mapping trộn (Auth + Profile) lại với nhau
        final Map<Long, ProfileDto> finalProfileMap = profileMap;
        List<UserResponse> data = usersPage.getContent().stream().map(user -> {
            ProfileDto profile = finalProfileMap.get(user.getId());
            return userMapper.toResponseAggregated(user, profile);
        }).collect(Collectors.toList());

        return new PageDTO<>(usersPage, data);
    }

    private static @NonNull Specification<Users> getUsersSpecification(Integer roleType, Integer status, List<Long> matchingUserIds) {
        final List<Long> finalMatchingUserIds = matchingUserIds;
        Specification<Users> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (finalMatchingUserIds != null) {
                predicate = cb.and(predicate, root.get("id").in(finalMatchingUserIds));
            }
            if (roleType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("roleType"), roleType));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
        return spec;
    }

    public UserResponse getUserById(Long id) {
        Users user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ProfileDto profile = null;
        try {
            profile = profileServiceClient.getProfileByUserId(id).getResult();
        } catch (Exception e) {
            log.warn("Không thể lấy profile cho userId {}: {}", id, e.getMessage());
        }

        return userMapper.toResponseAggregated(user, profile);
    }

    public UserResponse getUserByEmail(String email) {
        Users user = userRepository.findByEmail(email).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return getUserById(user.getId());
    }

    @Transactional
    public void deleteUser(Long id) {
        Users user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String tokenKeycloakId = jwt.getClaimAsString("sub");
        boolean isOwner = user.getKeycloakId() != null && user.getKeycloakId().equals(tokenKeycloakId);

        user.setStatus(isOwner ? 0 : 2);

        if (user.getKeycloakId() != null) {
            redisService.set("BLOCKED_USER:" + user.getKeycloakId(), "BLOCKED", 7 * 24 * 60);
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation userRep = userResource.toRepresentation();
                userRep.setEnabled(false);
                userResource.update(userRep);
            } catch (Exception e) {
                log.error("Lỗi khóa user Keycloak: {}", e.getMessage());
            }
        }

        userRepository.save(user);

        if (!isOwner) {
            eventPublisher.publishEvent(new UserDisabledEvent(user.getId(), "Tài khoản bị vô hiệu hóa", "Đã bị vô hiệu hóa bởi Admin.", 99));
        }
    }

    @Transactional
    public void activateUser(Long id) {
        Users user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setStatus(1);

        if (user.getKeycloakId() != null) {
            redisService.delete("BLOCKED_USER:" + user.getKeycloakId());
            try {
                UserResource userResource = keycloak.realm(realm).users().get(user.getKeycloakId());
                UserRepresentation userRep = userResource.toRepresentation();
                userRep.setEnabled(true);
                userResource.update(userRep);
            } catch (Exception e) {
                log.error("Lỗi mở khóa user Keycloak: {}", e.getMessage());
            }
        }
        userRepository.save(user);
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

            // 1. Lưu core user
            Users newUser = new Users();
            newUser.setKeycloakId(keycloakId);
            newUser.setUsername(jwt.getClaimAsString("preferred_username"));
            newUser.setEmail(jwt.getClaimAsString("email"));
            newUser.setRoleType(1);
            newUser.setStatus(1);
            newUser = userRepository.save(newUser);

            log.info("Đã đồng bộ core user: {}", newUser.getUsername());

            // 2. Truyền data qua Profile Service để tạo hồ sơ ban đầu
            try {
                CreateProfileDto profileDto = CreateProfileDto.builder()
                        .userId(newUser.getId())
                        .fullName(fullName.trim())
                        .build();
                profileServiceClient.createProfile(profileDto);
            } catch (Exception e) {
                log.error("Lỗi tạo profile ban đầu cho user {}: {}", newUser.getId(), e.getMessage());
            }
        }
    }

    public Map<String, Long> countUsersByRole(String keyword) {
        List<Long> matchingUserIds = null;

        if (keyword != null && !keyword.trim().isEmpty()) {
            try {
                matchingUserIds = profileServiceClient.searchUserIdsByCriteria(keyword, null, null).getResult();
                if (matchingUserIds == null || matchingUserIds.isEmpty()) {
                    return Map.of("student", 0L, "faculty", 0L, "admin", 0L);
                }
            } catch (Exception e) {
                log.error("Lỗi lấy list userId để đếm: {}", e.getMessage());
                return Map.of("student", 0L, "faculty", 0L, "admin", 0L);
            }
        }

        final List<Long> finalIds = matchingUserIds;
        Specification<Users> baseSpec = (root, query, cb) -> {
            if (finalIds == null) return cb.conjunction();
            return root.get("id").in(finalIds);
        };

        long studentCount = userRepository.count(baseSpec.and((root, q, cb) -> cb.equal(root.get("roleType"), 1)));
        long facultyCount = userRepository.count(baseSpec.and((root, q, cb) -> cb.equal(root.get("roleType"), 2)));
        long adminCount = userRepository.count(baseSpec.and((root, q, cb) -> cb.equal(root.get("roleType"), 3)));

        Map<String, Long> counts = new HashMap<>();
        counts.put("student", studentCount);
        counts.put("faculty", facultyCount);
        counts.put("admin", adminCount);
        return counts;
    }
}