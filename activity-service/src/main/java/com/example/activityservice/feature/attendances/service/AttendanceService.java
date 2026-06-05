package com.example.activityservice.feature.attendances.service;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.AttendanceStatisticsResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;
import com.example.activityservice.feature.attendances.dto.QRVerifyRequest;
import com.example.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.io.OutputStream;

public interface AttendanceService {
    AttendanceResponse checkIn(CheckInRequest request);
    
    // ============ NEW METHODS FOR UC FEATURES ============
    PageDTO<AttendanceResponse> getAttendancesBySession(Long activityId, Long sessionId, Pageable pageable);
    
    AttendanceResponse verifyAndCheckIn(QRVerifyRequest request);
    
    void exportAttendanceToExcel(Long activityId, Long sessionId, OutputStream outputStream) throws Exception;
    
    AttendanceStatisticsResponse getStatistics(Long activityId, Long sessionId);
}
