package com.example.activityservice.feature.registration.service;

import com.example.activityservice.feature.registration.dto.RegistrationQRResponse;
import com.example.activityservice.feature.registration.dto.RegistrationRequest;
import com.example.activityservice.feature.registration.dto.RegistrationResponse;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.io.OutputStream;
import java.util.List;

public interface RegistrationService {
    RegistrationResponse getMyStatusByActivity(Long activityId);

    RegistrationResponse register(RegistrationRequest request);

    PageDTO<RegistrationResponse> getParticipants(Long activityId, String keyword, String status, Pageable pageable);

    RegistrationResponse updateStatus(Long id, Integer status);

    RegistrationResponse cancelByActivityId(Long activityId, String reason);

    RegistrationResponse cancel(Long registrationId, String reason);

    RegistrationQRResponse getQRCode(Long registrationId);

    RegistrationResponse updateSessions(Long registrationId, List<Long> sessionIds);

    void exportToExcel(Long activityId, String keyword, String status, OutputStream outputStream);

    List<RegistrationResponse> getMyRecords(Long semesterId);
}