package com.example;

import com.example.feature.service.impl.NotificationServiceImpl;
import com.example.feature.service.impl.NotificationDispatchServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration"
})
@EnableDiscoveryClient
@ComponentScan(basePackages = { "com.example.feature", "com.example.config" })
@Import({NotificationServiceImpl.class, NotificationDispatchServiceImpl.class})
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}