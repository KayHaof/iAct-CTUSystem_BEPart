package com.example.feignClient;

import com.example.common.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    // Gọi API public
    @PostMapping("/notifications/public")
    void sendPublicNotification(@RequestBody String message);

    // Gọi API private
    @PostMapping("/notifications/private")
    void sendPrivateNotification(@RequestParam("userId") String userId, @RequestBody String message);
}
