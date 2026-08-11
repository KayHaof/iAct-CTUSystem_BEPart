package com.example.activityservice.feature.registration.service;

import com.example.activityservice.feature.registration.dto.RegistrationRequest;
import com.example.activityservice.feature.registration.dto.RegistrationResponse;
import com.example.activityservice.feature.registration.dto.AbsenceReviewRequest;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.io.OutputStream;
import java.util.List;

public interface RegistrationService {
    RegistrationResponse getMyStatusByActivity(Long activityId);

    RegistrationResponse register(RegistrationRequest request);

    PageDTO<RegistrationResponse> getParticipants(
            Long activityId,
            String keyword,
            String status,
            String academicYear,
            Pageable pageable);

    List<String> getParticipantAcademicYears(Long activityId);

    RegistrationResponse updateStatus(Long id, Integer status);

    RegistrationResponse updateStatus(Long id, Integer status, boolean processViolation);

    RegistrationResponse cancelByActivityId(Long activityId, String reason);

    RegistrationResponse cancel(Long registrationId, String reason);

    RegistrationResponse updateSessions(Long registrationId, List<Long> sessionIds);

    void exportToExcel(Long activityId, String keyword, String status, String academicYear, OutputStream outputStream);

    List<RegistrationResponse> getMyRecords(Long semesterId);

    PageDTO<RegistrationResponse> getAbsentParticipants(
            Long activityId,
            String keyword,
            String academicYear,
            Boolean reviewed,
            Pageable pageable);

    PageDTO<RegistrationResponse> getStudentsWithoutProof(Long activityId, Pageable pageable);

    RegistrationResponse reviewAbsence(Long registrationId, AbsenceReviewRequest request);
}
