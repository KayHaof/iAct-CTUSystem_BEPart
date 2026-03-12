package com.example.feature.registration.service;

import com.example.dto.PageDTO;
import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;
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

//    List<RegistrationResponse> getByActivity(Long activityId);

    void exportToExcel(Long activityId, String keyword, String status, OutputStream outputStream);
}