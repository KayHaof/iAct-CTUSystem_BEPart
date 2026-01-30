package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendGlobalNotification(String message) {
        messagingTemplate.convertAndSend("/topic/global", message);
        System.out.println("Đã gửi thông báo chung: " + message);
    }

    public void sendPrivateNotification(String userId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/specific-user",
                message
        );
        System.out.println("Đã gửi riêng cho " + userId + ": " + message);
    }
}
