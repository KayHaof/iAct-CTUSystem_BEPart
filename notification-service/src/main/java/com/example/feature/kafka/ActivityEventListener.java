package com.example.feature.kafka;

import com.example.event.ActivityDeletedEvent;
import com.example.feature.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityEventListener {
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "iact.activity.deleted", groupId = "notification-group")
    @Transactional
    public void handleActivityDeletedEvent(ActivityDeletedEvent event) {
        log.info("KAFKA NHẬN LỆNH: Yêu cầu xóa thông báo của Activity ID = {}", event.getActivityId());

        try {
            notificationRepository.deleteByActivityId(event.getActivityId());
            log.info("Đã dọn dẹp xong thông báo cho Activity ID = {}", event.getActivityId());

        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp thông báo: {}", e.getMessage());
        }
    }
}
