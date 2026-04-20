package com.example.feature.users.service;

import com.example.common.model.IdtLocalProfile;
import com.example.common.repository.LocalProfileRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.users.dto.*;
import com.example.feature.users.event.UserDisabledEvent;
import com.example.feature.users.mapper.UserProfileMapper;
import com.example.feature.users.model.Users;
import com.example.feature.users.repository.UserRepository;
import com.example.service.BaseRedisService;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final LocalProfileRepository localProfileRepository;
    private final UserProfileMapper userMapper;

    private final BaseRedisService redisService;
    private final Keycloak keycloak;
    private final ApplicationEventPublisher eventPublisher;
    private final String realm = "myRealm";

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserResponse getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Chưa đăng nhập");
        }

        String keycloakId = jwt.getClaimAsString("sub");
        if (keycloakId == null) throw new AppException(ErrorCode.UNAUTHENTICATED, "Token không hợp lệ");

        Users user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ProfileDto profile = localProfileRepository.findByUserId(user.getId())
                .map(userMapper::toProfileDto)
                .orElse(null);

        return userMapper.toResponseAggregated(user, profile);
    }


    public PageDTO<UserResponse> getUsers(int page, int size, String keyword, Integer roleType, Long departmentId, Integer status, Long classId) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, Sort.by("id").descending());
        List<Long> matchingUserIds = null;

        if (keyword != null || departmentId != null || classId != null) {
            matchingUserIds = localProfileRepository.findUserIdsByCriteria(keyword, departmentId, classId);
            if (matchingUserIds.isEmpty()) return new PageDTO<>(Page.empty(), Collections.emptyList());
        }

        final List<Long> finalMatchingUserIds = matchingUserIds;
        Specification<Users> spec = (root, query, cb) -> {
            Predicate p = cb.conjunction();
            if (finalMatchingUserIds != null) p = cb.and(p, root.get("id").in(finalMatchingUserIds));
            if (roleType != null) p = cb.and(p, cb.equal(root.get("roleType"), roleType));
            if (status != null) p = cb.and(p, cb.equal(root.get("status"), status));
            return p;
        };

        Page<Users> usersPage = userRepository.findAll(spec, pageable);
        List<Long> userIds = usersPage.getContent().stream().map(Users::getId).toList();

        Map<Long, ProfileDto> profileMap = localProfileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(IdtLocalProfile::getUserId, userMapper::toProfileDto));

        List<UserResponse> data = usersPage.getContent().stream()
                .map(u -> userMapper.toResponseAggregated(u, profileMap.get(u.getId())))
                .collect(Collectors.toList());

        return new PageDTO<>(usersPage, data);
    }

    public UserResponse getUserById(Long id) {
        Users user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        ProfileDto profile = localProfileRepository.findByUserId(id).map(userMapper::toProfileDto).orElse(null);
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
        assert authentication != null;
        Jwt jwt = (Jwt) authentication.getPrincipal();
        assert jwt != null;
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
        Users user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getKeycloakId() == null) throw new RuntimeException("Tài khoản này chưa được đồng bộ với Keycloak!");

        try {
            keycloak.realm(realm).users().get(user.getKeycloakId()).executeActionsEmail(Collections.singletonList("UPDATE_PASSWORD"));
            log.info("Đã yêu cầu Keycloak gửi email reset password cho user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email reset mật khẩu: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_ACTION, "Lỗi cấu hình SMTP.");
        }
    }

    public Map<String, Long> countUsersByRole(String keyword) {
        List<Long> matchingUserIds = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            matchingUserIds = localProfileRepository.findUserIdsByCriteria(keyword, null, null);
            if (matchingUserIds == null || matchingUserIds.isEmpty()) {
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

    @Transactional
    public void syncUserFromKeycloak(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        if (!userRepository.existsByKeycloakId(keycloakId)) {
            Users newUser = userRepository.save(Users.builder()
                    .keycloakId(keycloakId)
                    .username(jwt.getClaimAsString("preferred_username"))
                    .email(jwt.getClaimAsString("email"))
                    .roleType(1).status(1).build());

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", newUser.getId());
            payload.put("fullName", jwt.getClaimAsString("name"));
            payload.put("username", newUser.getUsername());

            try {
                String jsonString = objectMapper.writeValueAsString(payload);
                kafkaTemplate.send("user-created-topic", jsonString);
                log.info("Đã bắn Kafka (String) báo tạo user mới: {}", newUser.getId());
            } catch (Exception e) {
                log.error("Lỗi parse JSON khi gửi Kafka: {}", e.getMessage());
            }
        }
    }

    public UserResponse getUserByUsername(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy người dùng có username này"));

        ProfileDto profile = localProfileRepository.findByUserId(user.getId())
                .map(userMapper::toProfileDto)
                .orElse(null);

        return userMapper.toResponseAggregated(user, profile);
    }
}