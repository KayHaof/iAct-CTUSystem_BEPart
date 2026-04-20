package com.example.feature.user_profile.kafka;

import com.example.feature.user_profile.model.StudentProfile;
import com.example.feature.user_profile.repository.StudentProfileRepository;
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
public class UserCreatedConsumer {

    private final StudentProfileRepository studentRepo;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-created-topic", groupId = "profile-group-v1")
    public void consumeUserCreated(String message) {
        try {
            log.info("📥 [PROFILE] TIN NHẮN TẠO USER BAY TỚI: {}", message);

            // Dịch String JSON về Map
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});

            Long userId = Long.valueOf(data.get("userId").toString());
            String fullName = data.get("fullName") != null ? data.get("fullName").toString() : "Tân binh iAct";

            // Kiểm tra xem đã có chưa (đề phòng Kafka gửi trùng)
            if (!studentRepo.existsById(userId)) {
                StudentProfile newProfile = StudentProfile.builder()
                        .userId(userId)
                        .fullName(fullName)
                        .studentCode("N/A_" + userId) // Tạo mã SV tạm thời
                        .build();

                studentRepo.save(newProfile);
                log.info("[PROFILE] ĐÃ TẠO HỒ SƠ MỚI CHO USER ID: {}", userId);
            } else {
                log.info("[PROFILE] Bỏ qua, hồ sơ User ID {} đã tồn tại.", userId);
            }

        } catch (Exception e) {
            log.error("[PROFILE] Lỗi khi xử lý tạo hồ sơ mới: {}", e.getMessage());
        }
    }
}
