package com.example.feature.service.impl;

import com.example.feature.model.Notifications;
import com.example.feature.repository.NotificationRepository;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notifications save(Notifications notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notifications> getAllByUserId(Long userId) {
        // Dùng hàm fetch để tối ưu query (lấy luôn thông tin Activity đi kèm)
        return notificationRepository.findAllByUserIdFetched(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        Notifications notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));

        // Chỉ update nếu đang là false
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
