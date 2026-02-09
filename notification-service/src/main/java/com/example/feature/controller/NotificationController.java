package com.example.feature.controller;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.mapper.NotificationMapper;
import com.example.feature.model.Notifications;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @PostMapping
    public void createNotification(@RequestBody NotificationRequest request) {
        // 1. Lưu vào DB (để user có lịch sử thông báo, kể cả khi bị khóa)
        Notifications entity = notificationMapper.toEntity(request);
        Notifications savedEntity = notificationService.save(entity);
        NotificationResponse response = notificationMapper.toResponse(savedEntity);

        // 2. Phân loại gửi WebSocket
        if (request.getUserId() != null) {
            // --- TRƯỜNG HỢP: RIÊNG TƯ (PRIVATE / LOCK USER) ---
            String userIdStr = String.valueOf(request.getUserId());

            // LỜI KHUYÊN: Dùng convertAndSend tới topic cụ thể sẽ dễ debug hơn dùng convertAndSendToUser
            // Frontend sẽ subscribe vào: /topic/user/{userId}
            messagingTemplate.convertAndSend(
                    "/topic/user/" + userIdStr,
                    response
            );

            System.out.println("LOG: Đã gửi thông báo (User ID " + userIdStr + ") - Type: " + request.getType());

        } else {
            // --- TRƯỜNG HỢP: CÔNG KHAI (PUBLIC) ---
            // Frontend subscribe vào: /topic/notifications
            messagingTemplate.convertAndSend("/topic/notifications", response);
            System.out.println("LOG: Đã gửi thông báo công khai.");
        }
    }
}