package com.example.feature.activities.kafka;

import com.example.common.entity.Users;
import com.example.common.repository.LocalUserRepository;
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

    private final LocalUserRepository localUserRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-created-topic", groupId = "activity-group-v1")
    public void consumeUserCreated(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});

            Long userId = Long.valueOf(data.get("userId").toString());
            String username = data.get("username").toString();

            // Kiểm tra xem đã có chưa, chưa có thì đúc vỏ bọc mới
            Users user = localUserRepository.findById(userId).orElse(new Users());

            // [CỰC KỲ QUAN TRỌNG] Phải ép cứng ID này bằng đúng ID của Identity gửi qua
            // để đảm bảo tính nhất quán dữ liệu giữa 2 Service!
            user.setId(userId);
            user.setUsername(username);

            localUserRepository.save(user);
            log.info("[ACTIVITY] Đã đúc thành công vỏ bọc User (ID: {}, Username: {})", userId, username);

        } catch (Exception e) {
            log.error("Lỗi đồng bộ Kafka qua Activity: {}", e.getMessage());
        }
    }
}
