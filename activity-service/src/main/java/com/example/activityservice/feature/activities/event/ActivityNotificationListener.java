package com.example.activityservice.feature.activities.event;

import com.example.activityservice.common.dto.NotificationRequest;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityNotificationListener {
    private final ActivityMapper activityMapper;
    private final NotificationCommandProducer notificationCommandProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleActivityCreated(ActivityCreatedEvent event) {
        try {
            log.info("Bắt đầu bắn event thông báo qua Kafka cho Activity ID: {}", event.getActivity().getId());

            NotificationRequest request = activityMapper.toNotificationRequest(
                    event.getActivity(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getType()
            );

            notificationCommandProducer.publishCreated(request);

            log.info("Bắn event thông báo vào Kafka thành công!");
        } catch (Exception e) {
            log.error("Lỗi bắn event Kafka (nhưng Activity đã lưu DB thành công): {}", e.getMessage());
        }
    }
}
