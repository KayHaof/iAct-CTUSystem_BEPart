package com.example.feature.kafka;

import com.example.event.kafka.KafkaEventTypes;
import com.example.event.kafka.KafkaTopics;
import com.example.feature.dto.NotificationRequest;
import com.example.feature.repository.NotificationRepository;
import com.example.feature.service.NotificationDispatchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityBusinessEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {
            KafkaTopics.ACTIVITY_CREATED,
            KafkaTopics.ACTIVITY_SUBMITTED,
            KafkaTopics.ACTIVITY_UPDATED,
            KafkaTopics.ACTIVITY_APPROVED,
            KafkaTopics.ACTIVITY_REJECTED,
            KafkaTopics.ACTIVITY_CANCELLED,
            KafkaTopics.REGISTRATION_CREATED,
            KafkaTopics.REGISTRATION_CANCELLED,
            KafkaTopics.ATTENDANCE_CHECKED_IN,
            KafkaTopics.PROOF_SUBMITTED,
            KafkaTopics.PROOF_APPROVED,
            KafkaTopics.PROOF_REJECTED,
            KafkaTopics.POINT_AWARDED,
            KafkaTopics.POINT_RECALCULATED,
            KafkaTopics.POINT_REVOKED
    }, groupId = "notification-business-v1")
    @Transactional
    public void handleBusinessEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode root = objectMapper.readTree(message);
            NotificationRequest request = toNotificationRequest(root, topic);
            if (request == null) {
                log.info("Business event does not require notification. topic={}", topic);
                return;
            }
            notificationDispatchService.createAndDispatch(request);
        } catch (Exception e) {
            log.error("Failed to handle business notification event. topic={}, error={}",
                    topic, e.getMessage(), e);
            throw new IllegalStateException("Business notification event failed", e);
        }
    }

    @KafkaListener(topics = { KafkaTopics.ACTIVITY_DELETED,
            KafkaTopics.ACTIVITY_DRAFT_EXPIRED }, groupId = "notification-cleanup-v1")
    @Transactional
    public void handleActivityCleanup(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode root = objectMapper.readTree(message);
            Long activityId = requiredLong(payload(root), "activityId");
            notificationRepository.deleteByActivityId(activityId);
            log.info("Activity cleanup handled. topic={}, eventId={}, activityId={}",
                    topic, text(root, "eventId"), activityId);
        } catch (Exception e) {
            log.error("Failed to handle activity cleanup. topic={}, error={}", topic, e.getMessage(), e);
            throw new IllegalStateException("Activity cleanup failed", e);
        }
    }

    private NotificationRequest toNotificationRequest(JsonNode root, String topic) {
        String eventType = text(root, "eventType");
        JsonNode payload = payload(root);
        NotificationRequest request = new NotificationRequest();
        request.setActivityId(optionalLong(payload, "activityId"));
        request.setSourceEventId(text(root, "eventId"));
        request.setSourceTopic(topic);
        request.setReferenceType(text(root, "aggregateType"));

        switch (eventType) {
            case KafkaEventTypes.ACTIVITY_CREATED -> {
                return null;
            }
            case KafkaEventTypes.ACTIVITY_SUBMITTED -> {
                request.setUserId(null);
                request.setTitle("Hoạt động chờ duyệt");
                request.setMessage("Hoạt động '" + title(payload) + "' vừa được gửi lên chờ duyệt.");
                request.setType(2);
            }
            case KafkaEventTypes.ACTIVITY_UPDATED -> {
                request.setUserId(optionalLong(payload, "ownerUserId"));
                request.setTitle("Cập nhật hoạt động");
                request.setMessage("Hoạt động '" + title(payload) + "' vừa được cập nhật.");
                request.setType(2);
            }
            case KafkaEventTypes.ACTIVITY_APPROVED -> {
                request.setUserId(optionalLong(payload, "ownerUserId"));
                request.setTitle("Hoạt động đã được duyệt");
                request.setMessage("Hoạt động '" + title(payload) + "' đã được phê duyệt.");
                request.setType(1);
            }
            case KafkaEventTypes.ACTIVITY_REJECTED -> {
                request.setUserId(optionalLong(payload, "ownerUserId"));
                request.setTitle("Hoạt động bị từ chối");
                request.setMessage("Hoạt động '" + title(payload) + "' bị từ chối. Lý do: "
                        + defaultText(payload, "reason", "Không có lý do"));
                request.setType(3);
            }
            case KafkaEventTypes.ACTIVITY_CANCELLED -> {
                request.setUserId(optionalLong(payload, "ownerUserId"));
                request.setTitle("Hoạt động đã bị hủy");
                request.setMessage("Hoạt động '" + title(payload) + "' đã bị hủy. Lý do: "
                        + defaultText(payload, "reason", "Sự cố ngoài ý muốn"));
                request.setType(3);
            }
            case KafkaEventTypes.REGISTRATION_CREATED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Đăng ký thành công");
                request.setMessage("Bạn đã đăng ký thành công hoạt động: " + activityTitle(payload));
                request.setType(1);
            }
            case KafkaEventTypes.REGISTRATION_CANCELLED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Hủy đăng ký thành công");
                request.setMessage(cancelMessage(payload));
                request.setType(2);
            }
            case KafkaEventTypes.ATTENDANCE_CHECKED_IN -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Check-in thành công");
                request.setMessage("Bạn đã check-in thành công buổi: "
                        + defaultText(payload, "sessionTitle", "Hoạt động")
                        + " của hoạt động: " + activityTitle(payload));
                request.setType(3);
            }
            case KafkaEventTypes.PROOF_SUBMITTED -> {
                request.setUserId(optionalLong(payload, "ownerUserId"));
                request.setTitle("Minh chứng mới cần duyệt");
                request.setMessage("Sinh viên vừa nộp minh chứng cho hoạt động: " + activityTitle(payload));
                request.setType(2);
            }
            case KafkaEventTypes.PROOF_APPROVED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Minh chứng đã được duyệt");
                request.setMessage("Minh chứng của bạn cho hoạt động " + activityTitle(payload) + " đã được duyệt.");
                request.setType(1);
            }
            case KafkaEventTypes.PROOF_REJECTED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Minh chứng bị từ chối");
                request.setMessage("Minh chứng của bạn cho hoạt động " + activityTitle(payload)
                        + " bị từ chối. Lý do: " + defaultText(payload, "reason", "Không có lý do"));
                request.setType(3);
            }
            case KafkaEventTypes.POINT_AWARDED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Điểm rèn luyện đã được ghi nhận");
                request.setMessage("Bạn đã được ghi nhận điểm từ hoạt động: " + activityTitle(payload));
                request.setType(1);
            }
            case KafkaEventTypes.POINT_RECALCULATED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Điểm rèn luyện đã được tính lại");
                request.setMessage("Điểm của bạn vừa được tính lại từ hoạt động: " + activityTitle(payload));
                request.setType(2);
            }
            case KafkaEventTypes.POINT_REVOKED -> {
                request.setUserId(requiredLong(payload, "userId"));
                request.setTitle("Điểm rèn luyện bị thu hồi");
                request.setMessage("Điểm từ hoạt động " + activityTitle(payload)
                        + " đã bị thu hồi. Lý do: " + defaultText(payload, "reason", "Minh chứng không hợp lệ"));
                request.setType(3);
            }
            default -> {
                return null;
            }
        }
        request.setContent(request.getMessage());
        return request;
    }

    private JsonNode payload(JsonNode root) {
        return root.has("payload") ? root.get("payload") : root;
    }

    private String title(JsonNode payload) {
        return defaultText(payload, "title", "Không có tiêu đề");
    }

    private String activityTitle(JsonNode payload) {
        return defaultText(payload, "activityTitle", title(payload));
    }

    private String cancelMessage(JsonNode payload) {
        String base = "Bạn đã hủy đăng ký hoạt động: " + activityTitle(payload);
        String reason = text(payload, "reason");
        return reason == null || reason.isBlank() ? base : base + ". Lý do: " + reason;
    }

    private Long optionalLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private Long requiredLong(JsonNode node, String field) {
        Long value = optionalLong(node, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String defaultText(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
