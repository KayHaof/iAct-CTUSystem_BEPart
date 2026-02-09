package com.example.feature.service;

import com.example.feature.model.Notifications;
import java.util.List;

public interface NotificationService {
    Notifications save(Notifications notification);
    List<Notifications> getAllByUserId(Long userId);
    void markAsRead(Long id);
    long countUnread(Long userId);
}