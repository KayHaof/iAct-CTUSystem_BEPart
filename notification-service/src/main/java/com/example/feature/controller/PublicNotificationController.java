package com.example.feature.controller;

import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.feature.dto.NotificationResponse;
import com.example.feature.dto.UrgentNotificationRequest;
import com.example.feature.model.Notifications;
import com.example.feature.service.NotificationDispatchService;
import com.example.feature.service.NotificationService;
import com.example.feature.service.NotificationUserResolver;
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
    private final NotificationUserResolver userResolver;

    /**
     * UC11: Lấy danh sách thông báo của người dùng
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageDTO<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead) {
        
        Long userId = userResolver.resolveCurrentUserId(jwt);
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Notifications> notifications = notificationService.getNotifications(userId, isRead, pageable);
        PageDTO<NotificationResponse> result = new PageDTO<>();
        result.setPageNumber(page);
        result.setPageSize(size);
        result.setTotalPage(notifications.getTotalPages());
        result.setTotalRows(notifications.getTotalElements());
        result.setData(notifications.getContent().stream().map(this::toResponse).toList());
        result.setLast(notifications.isLast());
        
        return ApiResponse.success(result);
    }

    /**
     * UC11: Lấy số lượng thông báo chưa đọc
     */
    @GetMapping("/count-unread")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = userResolver.resolveCurrentUserId(jwt);
        return ApiResponse.success(notificationService.countUnread(userId));
    }

    /**
     * UC11: Đánh dấu một thông báo đã đọc
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = userResolver.resolveCurrentUserId(jwt);
        notificationService.markAsRead(id, userId);
        return ApiResponse.of(200, "Đã đánh dấu đã đọc", null);
    }

    /**
     * UC11: Đánh dấu tất cả thông báo đã đọc
     */
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        Long userId = userResolver.resolveCurrentUserId(jwt);
        notificationService.markAllAsRead(userId);
        return ApiResponse.of(200, "Đã đánh dấu tất cả đã đọc", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = userResolver.resolveCurrentUserId(jwt);
        notificationService.deleteNotification(id, userId);
        return ApiResponse.of(200, "Đã xóa thông báo", null);
    }

    /**
     * UC19: Gửi thông báo khẩn cấp đến sinh viên
     */
    @PostMapping("/urgent")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> sendUrgentNotification(
            @RequestBody UrgentNotificationRequest request) {

        int count = dispatchService.sendUrgentNotification(request);
        return ApiResponse.of(200, "Đã gửi thông báo đến " + count + " sinh viên thành công", count);
    }

    /**
     * UC11: Lấy chi tiết một thông báo
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationResponse> getNotificationById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = userResolver.resolveCurrentUserId(jwt);
        Notifications notification = notificationService.getById(id, userId);
        return ApiResponse.success(toResponse(notification));
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
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .sourceEventId(notification.getSourceEventId())
                .sourceTopic(notification.getSourceTopic())
                .build();
    }
}
