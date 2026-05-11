package com.example.feature.service;

import com.example.feature.dto.NotificationRequest;
import com.example.feature.dto.NotificationResponse;

public interface NotificationDispatchService {
    NotificationResponse createAndDispatch(NotificationRequest request);
}

