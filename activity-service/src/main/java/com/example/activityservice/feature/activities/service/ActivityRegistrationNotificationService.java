package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.proofs.repository.ProofRepository;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.event.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityRegistrationNotificationService {

    private static final int STATUS_APPROVED = 1;
    private static final int NOTIFICATION_TYPE_SUCCESS = 1;
    private static final int NOTIFICATION_TYPE_INFO = 2;
    private static final String REGISTRATION_OPEN_REFERENCE_TYPE = "activity-registration-open";
    private static final String ACTIVITY_START_REMINDER_REFERENCE_TYPE = "activity-start-reminder";
    private static final String SESSION_ACTION_REMINDER_SOURCE_TYPE = "activity-session-action-reminder";
    private static final String CHECK_IN_REMINDER_REFERENCE_TYPE = "activity-session-check-in-reminder";
    private static final String CHECK_OUT_REMINDER_REFERENCE_TYPE = "activity-session-check-out-reminder";
    private static final String PROOF_REMINDER_REFERENCE_TYPE = "activity-session-proof-reminder";
    private static final int SESSION_ACTION_REMINDER_MINUTES = 15;

    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final ProofRepository proofRepository;
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

    @Transactional(readOnly = true)
    public void publishExpiringSessionActionReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderThreshold = now.plusMinutes(SESSION_ACTION_REMINDER_MINUTES);
        List<Object[]> rows = registrationRepository.findActiveRegistrationsWithSchedulesEndingSoon(
                now,
                reminderThreshold);
        if (rows.isEmpty()) {
            return;
        }

        Map<Long, Registrations> registrationsById = new LinkedHashMap<>();
        Map<SessionReminderKey, ActivitySchedule> schedulesByRegistration = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row.length < 2 || !(row[0] instanceof Registrations registration)
                    || !(row[1] instanceof ActivitySchedule schedule)
                    || registration.getId() == null || schedule.getId() == null) {
                continue;
            }
            registrationsById.putIfAbsent(registration.getId(), registration);
            schedulesByRegistration.putIfAbsent(
                    new SessionReminderKey(registration.getId(), schedule.getId()),
                    schedule);
        }

        if (schedulesByRegistration.isEmpty()) {
            return;
        }

        List<Registrations> registrations = new ArrayList<>(registrationsById.values());
        Map<SessionReminderKey, Attendances> attendancesBySession = new HashMap<>();
        for (Attendances attendance : attendanceRepository.findByRegistrationIn(registrations)) {
            if (attendance.getRegistration() == null || attendance.getSchedule() == null
                    || attendance.getRegistration().getId() == null || attendance.getSchedule().getId() == null) {
                continue;
            }
            attendancesBySession.put(
                    new SessionReminderKey(attendance.getRegistration().getId(), attendance.getSchedule().getId()),
                    attendance);
        }

        Set<Long> registrationIdsWithProofs = proofRepository.findRegistrationIdsWithProofs(registrationsById.keySet());
        Set<Long> proofRemindersPublished = new HashSet<>();
        for (Map.Entry<SessionReminderKey, ActivitySchedule> entry : schedulesByRegistration.entrySet()) {
            SessionReminderKey key = entry.getKey();
            Registrations registration = registrationsById.get(key.registrationId());
            ActivitySchedule schedule = entry.getValue();
            if (registration == null || registration.getStudent() == null
                    || registration.getStudent().getId() == null) {
                continue;
            }

            Attendances attendance = attendancesBySession.get(key);
            if (!Integer.valueOf(Registrations.STATUS_ATTENDED).equals(registration.getStatus())) {
                if (attendance == null || attendance.getCheckinTime() == null) {
                    publishSessionActionReminder(
                            registration,
                            schedule,
                            CHECK_IN_REMINDER_REFERENCE_TYPE,
                            "Sắp hết thời gian check-in",
                            "Buổi '" + schedule.getTitle() + "' của hoạt động '"
                                    + activityTitle(registration) + "' sẽ kết thúc sau 15 phút. "
                                    + "Bạn chưa check-in, vui lòng thực hiện ngay.");
                } else if (attendance.getCheckoutTime() == null) {
                    publishSessionActionReminder(
                            registration,
                            schedule,
                            CHECK_OUT_REMINDER_REFERENCE_TYPE,
                            "Sắp hết thời gian check-out",
                            "Buổi '" + schedule.getTitle() + "' của hoạt động '"
                                    + activityTitle(registration) + "' sẽ kết thúc sau 15 phút. "
                                    + "Bạn chưa check-out, vui lòng thực hiện ngay.");
                }
            }

            if (Integer.valueOf(Registrations.STATUS_ATTENDED).equals(registration.getStatus())
                    && !registrationIdsWithProofs.contains(registration.getId())
                    && proofRemindersPublished.add(registration.getId())) {
                publishSessionActionReminder(
                        registration,
                        schedule,
                        PROOF_REMINDER_REFERENCE_TYPE,
                        "Sắp hết thời gian nộp minh chứng",
                        "Buổi '" + schedule.getTitle() + "' của hoạt động '"
                                + activityTitle(registration) + "' sẽ kết thúc sau 15 phút. "
                                + "Bạn chưa nộp minh chứng, vui lòng thực hiện ngay.");
            }
        }
    }

    private void publishSessionActionReminder(
            Registrations registration,
            ActivitySchedule schedule,
            String referenceType,
            String title,
            String message) {
        Long studentId = registration.getStudent().getId();
        String sourceEventId = SESSION_ACTION_REMINDER_SOURCE_TYPE
                + ":registration:" + registration.getId()
                + ":schedule:" + schedule.getId()
                + ":action:" + referenceType;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userIds", List.of(studentId));
        payload.put("activityId", registration.getActivity() != null ? registration.getActivity().getId() : null);
        payload.put("title", title);
        payload.put("message", message);
        payload.put("content", message);
        payload.put("type", NOTIFICATION_TYPE_INFO);
        payload.put("referenceType", referenceType);
        payload.put("sourceTopic", KafkaTopics.NOTIFICATION_ACTIVITY_SESSION_ACTION_REMINDER_REQUESTED);
        payload.put("sourceEventId", sourceEventId);
        payload.put("scheduleId", schedule.getId());
        payload.put("scheduleTitle", schedule.getTitle());

        notificationCommandProducer.publishActivitySessionActionReminder(sourceEventId, payload);
    }

    private String activityTitle(Registrations registration) {
        return registration.getActivity() != null && registration.getActivity().getTitle() != null
                ? registration.getActivity().getTitle()
                : "Hoạt động";
    }

    private record SessionReminderKey(Long registrationId, Long scheduleId) {
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
