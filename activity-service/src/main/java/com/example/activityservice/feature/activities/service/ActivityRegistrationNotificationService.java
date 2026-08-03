package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.event.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityRegistrationNotificationService {

    private static final int STATUS_APPROVED = 1;
    private static final int NOTIFICATION_TYPE_SUCCESS = 1;
    private static final int NOTIFICATION_TYPE_INFO = 2;
    private static final String REGISTRATION_OPEN_REFERENCE_TYPE = "activity-registration-open";
    private static final String ACTIVITY_START_REMINDER_REFERENCE_TYPE = "activity-start-reminder";

    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationCommandProducer notificationCommandProducer;

    public void notifyIfRegistrationOpen(Activities activity) {
        if (activity == null || !Integer.valueOf(STATUS_APPROVED).equals(activity.getStatus())
                || !isRegistrationOpen(activity)) {
            return;
        }
        publishRegistrationOpen(activity);
    }

    public void publishRegistrationOpen(Activities activity) {
        List<Long> recipientIds = resolveRecipientIds(activity);
        if (recipientIds.isEmpty()) {
            log.info("No active students found for activity registration notification. activityId={}",
                    activity.getId());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userIds", recipientIds);
        payload.put("activityId", activity.getId());
        payload.put("title", "Hoạt động đã mở đăng ký");
        payload.put("message", "Hoạt động '" + activity.getTitle() + "' đã đến thời gian đăng ký.");
        payload.put("content", payload.get("message"));
        payload.put("type", NOTIFICATION_TYPE_SUCCESS);
        payload.put("referenceType", REGISTRATION_OPEN_REFERENCE_TYPE);
        payload.put("sourceTopic", KafkaTopics.ACTIVITY_APPROVED);
        payload.put("sourceEventId", registrationOpenSourceEventId(activity));

        notificationCommandProducer.publishBroadcastRequested(registrationOpenSourceEventId(activity), payload);
        log.info("Published registration-open notification command. activityId={}, recipients={}",
                activity.getId(), recipientIds.size());
    }

    public void publishActivityStartReminder(Activities activity) {
        List<Long> recipientIds = registrationRepository.findActiveRegisteredStudentIdsByActivityId(activity.getId());
        if (recipientIds.isEmpty()) {
            log.info("No registered students found for activity start reminder. activityId={}", activity.getId());
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userIds", recipientIds);
        payload.put("activityId", activity.getId());
        payload.put("title", "Hoạt động sắp diễn ra");
        payload.put("message", "Hoạt động '" + activity.getTitle()
                + "' sẽ diễn ra sau khoảng 30 phút. Vui lòng chuẩn bị tham gia đúng giờ.");
        payload.put("content", payload.get("message"));
        payload.put("type", NOTIFICATION_TYPE_INFO);
        payload.put("referenceType", ACTIVITY_START_REMINDER_REFERENCE_TYPE);
        payload.put("sourceTopic", KafkaTopics.NOTIFICATION_BROADCAST_REQUESTED);
        payload.put("sourceEventId", startReminderSourceEventId(activity));

        notificationCommandProducer.publishBroadcastRequested(startReminderSourceEventId(activity), payload);
        log.info("Published activity-start reminder command. activityId={}, recipients={}",
                activity.getId(), recipientIds.size());
    }

    private List<Long> resolveRecipientIds(Activities activity) {
        if (isDepartmentCreatedActivity(activity)) {
            return userRepository.findActiveStudentIdsByDepartmentId(activity.getDepartmentId());
        }
        if (isFacultyInternalActivity(activity)) {
            if (activity.getDepartmentId() == null) {
                return List.of();
            }
            return userRepository.findActiveStudentIdsByDepartmentId(activity.getDepartmentId());
        }
        if (isSystemWideActivity(activity)) {
            return userRepository.findActiveStudentIds();
        }
        return List.of();
    }

    private boolean isRegistrationOpen(Activities activity) {
        LocalDateTime now = LocalDateTime.now();
        return activity.getRegistrationStart() != null
                && activity.getRegistrationEnd() != null
                && !now.isBefore(activity.getRegistrationStart())
                && !now.isAfter(activity.getRegistrationEnd());
    }

    private boolean isFacultyInternalActivity(Activities activity) {
        return Boolean.TRUE.equals(activity.getIsFaculty()) && !Boolean.TRUE.equals(activity.getIsExternal());
    }

    private boolean isDepartmentCreatedActivity(Activities activity) {
        return activity != null
                && activity.getCreatedBy() != null
                && Integer.valueOf(2).equals(activity.getCreatedBy().getRoleType())
                && activity.getDepartmentId() != null;
    }

    private boolean isSystemWideActivity(Activities activity) {
        return Boolean.TRUE.equals(activity.getIsExternal()) || !Boolean.TRUE.equals(activity.getIsFaculty());
    }

    private String registrationOpenSourceEventId(Activities activity) {
        return REGISTRATION_OPEN_REFERENCE_TYPE + ":" + activity.getId();
    }

    private String startReminderSourceEventId(Activities activity) {
        return ACTIVITY_START_REMINDER_REFERENCE_TYPE + ":" + activity.getId();
    }
}
