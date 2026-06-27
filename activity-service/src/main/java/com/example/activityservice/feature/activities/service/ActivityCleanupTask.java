package com.example.activityservice.feature.activities.service;

import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.event.ActivityDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityCleanupTask {
    private final ActivityRepository activityRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ActivityEventProducer activityEventProducer;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanUpOldDrafts() {
        log.info("Starting old draft cleanup task...");

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Activities> oldDrafts = activityRepository.findByStatusAndUpdatedAtBefore(3, sevenDaysAgo);

        if (!oldDrafts.isEmpty()) {
            for (Activities draft : oldDrafts) {
                Long draftId = draft.getId();
                activityRepository.delete(draft);

                kafkaTemplate.send("iact.activity.deleted", new ActivityDeletedEvent(draftId));
                activityEventProducer.publishDraftExpired(draftId);
            }
            log.info("Cleaned up {} expired drafts and published cleanup events.", oldDrafts.size());
        } else {
            log.info("No expired drafts need cleanup.");
        }
    }
}

