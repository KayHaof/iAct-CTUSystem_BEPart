package com.example.feature.service.impl;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.kafka.NotificationLifecycleProducer;
import com.example.feature.mapper.NotificationMapper;
import com.example.feature.model.Notifications;
import com.example.feature.repository.NotificationPreferenceRepository;
import com.example.feature.repository.NotificationRepository;
import com.example.feature.service.NotificationDispatchService;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final NotificationLifecycleProducer notificationLifecycleProducer;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Override
    @Transactional
    public NotificationResponse createAndDispatch(NotificationRequest request) {
        Optional<Notifications> existing = findExistingEvent(request);
        if (existing.isPresent()) {
            log.info("Notification event already processed. sourceEventId={}, sourceTopic={}",
                    request.getSourceEventId(), request.getSourceTopic());
            return notificationMapper.toResponse(existing.get());
        }

        if (isDisabledByPreference(request)) {
            log.info("Notification skipped by preference. userId={}, type={}",
                    request.getUserId(), request.getType());
            return null;
        }

        if ((request.getMessage() == null || request.getMessage().isBlank()) && request.getContent() != null) {
            request.setMessage(request.getContent());
        }

        Notifications entity = notificationMapper.toEntity(request);
        Notifications savedEntity = notificationService.save(entity);
        NotificationResponse response = notificationMapper.toResponse(savedEntity);

        try {
            if (request.getUserId() != null) {
                String userIdStr = String.valueOf(request.getUserId());
                messagingTemplate.convertAndSend("/topic/user/" + userIdStr, response);
                log.info("Notification dispatched to user topic. userId={}, type={}", userIdStr, request.getType());
            } else {
                messagingTemplate.convertAndSend("/topic/notifications", response);
                log.info("Public notification dispatched. type={}", request.getType());
            }
        } catch (Exception e) {
            notificationLifecycleProducer.publishDeliveryFailed(savedEntity.getId(), savedEntity.getUserId(), e.getMessage());
            log.error("Notification WebSocket dispatch failed. notificationId={}, userId={}",
                    savedEntity.getId(), savedEntity.getUserId(), e);
        }

        notificationLifecycleProducer.publishDispatched(response);
        return response;
    }

    @Override
    @Transactional
    public int sendUrgentNotification(com.example.feature.dto.UrgentNotificationRequest request) {
        if (request.getUserIds() == null || request.getUserIds().length == 0) {
            NotificationRequest notification = new NotificationRequest();
            notification.setTitle(request.getTitle());
            notification.setMessage(request.getMessage());
            notification.setContent(request.getMessage());
            notification.setType(resolveUrgentType(request.getPriority()));
            notification.setActivityId(request.getActivityId());
            notification.setReferenceType("urgent");
            createAndDispatch(notification);
            return 1;
        }

        return (int) Arrays.stream(request.getUserIds())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .map(userId -> {
                    NotificationRequest notification = new NotificationRequest();
                    notification.setUserId(userId);
                    notification.setTitle(request.getTitle());
                    notification.setMessage(request.getMessage());
                    notification.setContent(request.getMessage());
                    notification.setType(resolveUrgentType(request.getPriority()));
                    notification.setActivityId(request.getActivityId());
                    notification.setReferenceType("urgent");
                    return createAndDispatch(notification);
                })
                .filter(Objects::nonNull)
                .count();
    }

    private Optional<Notifications> findExistingEvent(NotificationRequest request) {
        if (request.getSourceEventId() == null || request.getSourceTopic() == null) {
            return Optional.empty();
        }
        return notificationRepository.findBySourceEventIdAndSourceTopic(
                request.getSourceEventId(), request.getSourceTopic());
    }

    private boolean isDisabledByPreference(NotificationRequest request) {
        return request.getUserId() != null
                && request.getType() != null
                && notificationPreferenceRepository.existsByUserIdAndTypeAndIsEnabledFalse(
                request.getUserId(), request.getType());
    }

    private Integer resolveUrgentType(Integer priority) {
        return priority != null ? priority : 3;
    }
}

