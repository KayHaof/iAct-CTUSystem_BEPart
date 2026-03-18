package com.example.feignClient;

import com.example.common.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "notification-service",
        path = "/internal/notifications",
        configuration = FeignClientInterceptor.class
)
public interface NotificationClient {

    @PostMapping
    void createNotification(@RequestBody NotificationRequest request);
}