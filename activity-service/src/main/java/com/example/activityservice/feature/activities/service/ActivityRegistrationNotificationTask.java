package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityRegistrationNotificationTask {

    private final ActivityRepository activityRepository;
    private final ActivityRegistrationNotificationService notificationService;

    @Value("${app.notifications.activity-start-reminder-minutes:30}")
    private long activityStartReminderMinutes;

    // Quet thuong xuyen de phat thong bao gan dung moc registrationStart.
    @Scheduled(fixedDelayString = "${app.notifications.registration-open-scan-ms:1000}")
    public void notifyOpenRegistrationActivities() {
        List<Activities> openActivities =
                activityRepository.findApprovedActivitiesOpenForRegistration(LocalDateTime.now());
        for (Activities activity : openActivities) {
            notificationService.publishRegistrationOpen(activity);
        }
        if (!openActivities.isEmpty()) {
            log.info("Registration-open notification scan completed. activities={}", openActivities.size());
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.activity-start-reminder-scan-ms:60000}")
    public void notifyUpcomingActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<Activities> upcomingActivities = activityRepository.findApprovedActivitiesStartingSoon(
                now,
                now.plusMinutes(activityStartReminderMinutes));
        for (Activities activity : upcomingActivities) {
            notificationService.publishActivityStartReminder(activity);
        }
        if (!upcomingActivities.isEmpty()) {
            log.info("Activity-start reminder scan completed. activities={}", upcomingActivities.size());
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.session-action-reminder-scan-ms:60000}")
    public void notifyExpiringSessionActions() {
        notificationService.publishExpiringSessionActionReminders();
    }
}
