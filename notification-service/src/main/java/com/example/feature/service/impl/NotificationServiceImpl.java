package com.example.feature.service.impl;

import com.example.feature.model.Notifications;
import com.example.feature.repository.NotificationRepository;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return notificationRepository.findAllByUserIdFetched(userId);
    }

    @Override
    public Page<Notifications> getNotifications(Long userId, Boolean isRead, Pageable pageable) {
        if (isRead == null) {
            return notificationRepository.findByUserId(userId, pageable);
        }
        return notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        Notifications notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public Notifications getById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
    }
}
