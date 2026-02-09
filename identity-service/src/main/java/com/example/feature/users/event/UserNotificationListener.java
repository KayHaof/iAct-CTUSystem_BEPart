package com.example.feature.users.event;

import com.example.common.dto.NotificationRequest;
import com.example.feignClient.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserNotificationListener {

    private final NotificationClient notificationClient;

    // Phase = AFTER_COMMIT: Đợi transaction bên UserService commit xong mới chạy
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async // Chạy luồng riêng để không block request của admin
    public void handleUserDisabled(UserDisabledEvent event) {
        try {
            log.info("Bắt đầu gửi lệnh khóa tài khoản cho User ID: {}", event.getUserId());

            // Lưu ý: event.getUserId() đang là String (do lúc nãy mình define Event là String)
            // Nhưng DTO NotificationRequest cần Long, nên phải parse ép kiểu.
            Long userId = event.getUserId();

            NotificationRequest request = NotificationRequest.builder()
                    .userId(userId)           // Sửa từ recipientId -> userId cho khớp DTO
                    .title(event.getTitle())
                    .message(event.getMessage()) // Sửa từ content -> message cho khớp DTO
                    .type(event.getType())    // 99
                    .build();

            // Gọi Feign Client bắn qua Notification Service
            notificationClient.createNotification(request);

            log.info("Đã gửi lệnh khóa user sang Notification Service thành công!");

        } catch (NumberFormatException e) {
            log.error("Lỗi format ID user: {} không phải là số hợp lệ", event.getUserId());
        } catch (Exception e) {
            // Log lỗi nhưng không làm rollback transaction của việc xóa user (vì transaction đã commit rồi)
            log.error("Lỗi gửi thông báo khóa user: {}", e.getMessage());
        }
    }
}