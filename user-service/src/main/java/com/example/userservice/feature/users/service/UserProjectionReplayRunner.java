package com.example.userservice.feature.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.user-projection.replay-on-startup",
        havingValue = "true")
@Slf4j
public class UserProjectionReplayRunner implements ApplicationRunner {

    private final UserProjectionReplayService replayService;

    @Override
    public void run(ApplicationArguments args) {
        long scheduledEvents = replayService.replayAll();
        log.info("Đã lên lịch replay {} User snapshot khi startup", scheduledEvents);
    }
}
