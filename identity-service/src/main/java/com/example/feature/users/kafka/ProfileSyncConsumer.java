package com.example.feature.users.kafka;

import com.example.common.model.IdtLocalProfile;
import com.example.common.repository.LocalProfileRepository;
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
public class ProfileSyncConsumer {

    private final LocalProfileRepository localProfileRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "profile-updated-topic", groupId = "identity-group-v3")
    public void consumeProfileUpdate(String message) {
        log.info("TIN NHẮN KAFKA BAY TỚI (CẬP NHẬT): {}", message);
        processProfileData(message, "CẬP NHẬT");
    }

    @KafkaListener(topics = "profile-created-topic", groupId = "identity-group-v3")
    public void consumeProfileCreated(String message) {
        log.info("[IDENTITY] CÓ USER MỚI, CHUẨN BỊ TẠO IDTLOCAL: {}", message);
        processProfileData(message, "TẠO");
    }

    private void processProfileData(String message, String actionName) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});
            Long userId = Long.valueOf(data.get("userId").toString());

            IdtLocalProfile profile = localProfileRepository.findByUserId(userId)
                    .orElse(new IdtLocalProfile());

            profile.setUserId(userId);
            if (data.get("fullName") != null) profile.setFullName(data.get("fullName").toString());
            if (data.get("studentCode") != null) profile.setStudentCode(data.get("studentCode").toString());
            if (data.get("avatarUrl") != null) profile.setAvatarUrl(data.get("avatarUrl").toString());
            if (data.get("classId") != null) profile.setClassId(Long.valueOf(data.get("classId").toString()));
            if (data.get("departmentId") != null) profile.setDepartmentId(Long.valueOf(data.get("departmentId").toString()));
            if (data.get("classCode") != null) profile.setClassCode(data.get("classCode").toString());
            if (data.get("departmentName") != null) profile.setDepartmentName(data.get("departmentName").toString());

            localProfileRepository.save(profile);
            log.info("ĐÃ {} THÀNH CÔNG VÀO DB IDENTITY CHO USER: {}", actionName, userId);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý Kafka ({}): {}", actionName, e.getMessage());
        }
    }
}