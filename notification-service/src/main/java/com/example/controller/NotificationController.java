package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // API 1: Gửi thông báo cho TẤT CẢ (Public)
    @PostMapping("/public")
    public void sendToAll(@RequestBody String message) {
        messagingTemplate.convertAndSend("/topic/public", message);
        System.out.println("Gửi public: " + message);
    }

    // API 2: Gửi cho MỘT SINH VIÊN (Private)
    @PostMapping("/private")
    public void sendToUser(@RequestParam String userId, @RequestBody String message) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/private", message);
        System.out.println("Gửi cho " + userId + ": " + message);
    }
}