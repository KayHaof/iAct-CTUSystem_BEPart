package com.example.feature.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.dto.UrgentNotificationRequest;
import com.example.feature.model.Notifications;
import com.example.feature.service.NotificationDispatchService;
import com.example.feature.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class PublicNotificationController {

    private final NotificationService notificationService;
    private final NotificationDispatchService dispatchService;

    /**
     * UC11: Lay danh sach thong bao cua nguoi dung
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageDTO<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead) {
        
        Long userId = extractUserId(jwt);
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Notifications> notifications = notificationService.getNotifications(userId, isRead, pageable);
        PageDTO<NotificationResponse> result = PageDTO.<NotificationResponse>builder()
                .pageNumber(page)
                .totalPage(notifications.getTotalPages())
                .totalRows(notifications.getTotalElements())
                .data(notifications.getContent().stream().map(this::toResponse).toList())
                .build();
        
        return ApiResponse.success(result);
    }

    /**
     * UC11: Lay so luong thong bao chua doc
     */
    @GetMapping("/count-unread")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ApiResponse.success(notificationService.countUnread(userId));
    }

    /**
     * UC11: Danh dau mot thong bao da doc
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.of(200, "Da danh dau da doc", null);
    }

    /**
     * UC11: Danh dau tat ca thong bao da doc
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        notificationService.markAllAsRead(userId);
        return ApiResponse.of(200, "Da danh dau tat ca da doc", null);
    }

    /**
     * UC19: Gui thong bao khan cap den sinh vien
     */
    @PostMapping("/urgent")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ApiResponse<Integer> sendUrgentNotification(
            @RequestBody UrgentNotificationRequest request) {

        int count = dispatchService.sendUrgentNotification(request);
        return ApiResponse.of(200, "Da gui thong bao den " + count + " sinh vien thanh cong", count);
    }

    /**
     * UC11: Lay chi tiet mot thong bao
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> getNotificationById(@PathVariable Long id) {
        Notifications notification = notificationService.getById(id);
        return ApiResponse.success(toResponse(notification));
    }

    private Long extractUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            subject = jwt.getClaimAsString("sub");
        }
        return Long.parseLong(subject);
    }

    private NotificationResponse toResponse(Notifications notification) {
        if (notification == null) return null;
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .activityId(notification.getActivityId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
