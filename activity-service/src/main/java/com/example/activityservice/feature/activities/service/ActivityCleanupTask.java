package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.event.ActivityDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityCleanupTask {
    private final ActivityRepository activityRepository;

    // ĐÃ THÊM KAFKA TẠI ĐÂY (VÀ ĐÃ XÓA LocalNotificationRepository)
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanUpOldDrafts() {
        log.info("Bắt đầu tiến trình dọn dẹp bản nháp cũ...");

        // 1. Lấy mốc thời gian là 7 ngày trước tính từ thời điểm hiện tại
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 2. Tìm tất cả các bản nháp (status = 3) không được đụng tới trong 7 ngày qua
        List<Activities> oldDrafts = activityRepository.findByStatusAndUpdatedAtBefore(3, sevenDaysAgo);

        if (!oldDrafts.isEmpty()) {
            for (Activities draft : oldDrafts) {
                Long draftId = draft.getId();

                // ĐÃ SỬA: Xóa DB của Activity trước
                activityRepository.delete(draft);

                // ================= KAFKA: HÉT LÊN CHO CẢ LÀNG BIẾT =================
                ActivityDeletedEvent event = new ActivityDeletedEvent(draftId);
                // Bắn event xóa thông báo qua Kafka
                kafkaTemplate.send("iact.activity.deleted", event);
                // ===================================================================
            }
            log.info(">> Đã dọn dẹp thành công {} bản nháp quá hạn và bắn sự kiện dọn thông báo qua Kafka.", oldDrafts.size());
        } else {
            log.info("<< Không có bản nháp nào quá hạn cần dọn dẹp.");
        }
    }
}