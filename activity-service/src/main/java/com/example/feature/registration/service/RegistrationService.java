package com.example.feature.registration.service;

import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;

import java.util.List;

public interface RegistrationService {
    RegistrationResponse getMyStatusByActivity(Long activityId);

    RegistrationResponse register(RegistrationRequest request);

    RegistrationResponse cancelByActivityId(Long activityId, String reason);

    RegistrationResponse cancel(Long registrationId, String reason);

    List<RegistrationResponse> getByActivity(Long activityId);
}