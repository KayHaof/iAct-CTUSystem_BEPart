package com.example.feature.activities.event;

import com.example.common.dto.NotificationRequest;
import com.example.feature.activities.mapper.ActivityMapper;
import com.example.feignClient.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityNotificationListener {

    private final NotificationClient notificationClient;
    private final ActivityMapper activityMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleActivityCreated(ActivityCreatedEvent event) {
        try {
            log.info("Bắt đầu gửi thông báo cho Activity ID: {}", event.getActivity().getId());

            NotificationRequest request = activityMapper.toNotificationRequest(
                    event.getActivity(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType()
            );

            notificationClient.createNotification(request);

            log.info("Gửi thông báo thành công!");
        } catch (Exception e) {
            log.error("Lỗi gửi thông báo (nhưng Activity đã lưu thành công): {}", e.getMessage());
        }
    }
}
