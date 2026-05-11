package com.example.feature.controller;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationDispatchService notificationDispatchService;

    @PostMapping
    public void createNotification(@RequestBody NotificationRequest request) {
        notificationDispatchService.createAndDispatch(request);
    }
}