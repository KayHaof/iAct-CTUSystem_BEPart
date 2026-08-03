package com.example.feature.service;

import com.example.feature.model.Notifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    Notifications save(Notifications notification);
    List<Notifications> getAllByUserId(Long userId);
    Page<Notifications> getNotifications(Long userId, Boolean isRead, Pageable pageable);
    void markAsRead(Long id, Long userId);
    void markAllAsRead(Long userId);
    void deleteNotification(Long id, Long userId);
    long countUnread(Long userId);
    Notifications getById(Long id, Long userId);
}
