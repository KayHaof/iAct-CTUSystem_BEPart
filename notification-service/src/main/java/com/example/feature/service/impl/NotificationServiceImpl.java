package com.example.feature.service.impl;

import com.example.feature.kafka.NotificationLifecycleProducer;
import com.example.feature.model.Notifications;
import com.example.feature.repository.NotificationRepository;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationLifecycleProducer lifecycleProducer;

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
    public void markAsRead(Long id, Long userId) {
        Notifications notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
        ensureBelongsToUser(notification, userId);

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            lifecycleProducer.publishRead(notification.getId(), notification.getUserId(),
                    notification.getReadAt().toString());
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notifications> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        notificationRepository.markAllAsRead(userId);
        String readAt = LocalDateTime.now().toString();
        unreadNotifications.forEach(notification ->
                lifecycleProducer.publishRead(notification.getId(), notification.getUserId(), readAt));
    }

    @Override
    @Transactional
    public void deleteNotification(Long id, Long userId) {
        Notifications notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
        if (userId != null && notification.getUserId() != null && !notification.getUserId().equals(userId)) {
            throw new RuntimeException("Notification does not belong to current user");
        }
        Long targetUserId = notification.getUserId();
        notificationRepository.delete(notification);
        lifecycleProducer.publishDeleted(id, targetUserId);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public Notifications getById(Long id, Long userId) {
        Notifications notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + id));
        ensureBelongsToUser(notification, userId);
        return notification;
    }

    private void ensureBelongsToUser(Notifications notification, Long userId) {
        if (notification.getUserId() == null
                || userId == null
                || !notification.getUserId().equals(userId)) {
            throw new RuntimeException("Notification does not belong to current user");
        }
    }
}
