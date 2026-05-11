package com.example.feature.service.impl;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.mapper.NotificationMapper;
import com.example.feature.model.Notifications;
import com.example.feature.service.NotificationDispatchService;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public NotificationResponse createAndDispatch(NotificationRequest request) {
        Notifications entity = notificationMapper.toEntity(request);
        Notifications savedEntity = notificationService.save(entity);
        NotificationResponse response = notificationMapper.toResponse(savedEntity);

        if (request.getUserId() != null) {
            String userIdStr = String.valueOf(request.getUserId());
            messagingTemplate.convertAndSend("/topic/user/" + userIdStr, response);
            log.info("Notification dispatched to user topic. userId={}, type={}", userIdStr, request.getType());
        } else {
            messagingTemplate.convertAndSend("/topic/notifications", response);
            log.info("Public notification dispatched. type={}", request.getType());
        }

        return response;
    }
}

