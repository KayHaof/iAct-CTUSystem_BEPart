package com.example.feature.activities.service;

import com.example.common.repository.LocalNotificationRepository;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final LocalNotificationRepository notificationRepository;

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

                if (notificationRepository.existsByActivityId(draftId)) {
                    notificationRepository.deleteByActivityId(draftId);
                }

                activityRepository.delete(draft);
            }
            log.info(">> Đã dọn dẹp thành công {} bản nháp quá hạn.", oldDrafts.size());
        } else {
            log.info("<< Không có bản nháp nào quá hạn cần dọn dẹp.");
        }
    }
}
