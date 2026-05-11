package com.example.activityservice.feature.activities.kafka;

import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityUserSyncConsumer {

    private final UserRepository localUserRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-created-topic", groupId = "activity-group-v1")
    public void consumeUserCreated(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});

            Long userId = Long.valueOf(data.get("userId").toString());
            String username = data.get("username").toString();

            Users user = localUserRepository.findById(userId).orElse(new Users());

            user.setId(userId);
            user.setUsername(username);
            if (data.get("email") != null) {
                user.setEmail(String.valueOf(data.get("email")));
            }
            if (data.get("fullName") != null) {
                user.setFullName(String.valueOf(data.get("fullName")));
            }
            if (data.get("departmentId") != null) {
                user.setDepartmentId(Long.valueOf(String.valueOf(data.get("departmentId"))));
            }
            if (data.get("studentCode") != null) {
                user.setStudentCode(String.valueOf(data.get("studentCode")));
            }
            if (data.get("avatarUrl") != null) {
                user.setAvatarUrl(String.valueOf(data.get("avatarUrl")));
            }

            localUserRepository.save(user);
            log.info("[ACTIVITY] Đã đúc thành công vỏ bọc User (ID: {}, Username: {})", userId, username);

        } catch (Exception e) {
            log.error("Lỗi đồng bộ Kafka qua Activity: {}", e.getMessage());
        }
    }
}
