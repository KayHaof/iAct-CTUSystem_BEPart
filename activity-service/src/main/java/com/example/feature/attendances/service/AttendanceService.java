package com.example.feature.attendances.service;

import com.example.feature.attendances.dto.AttendanceResponse;
import com.example.feature.attendances.dto.CheckInRequest;

public interface AttendanceService {
    AttendanceResponse checkIn(CheckInRequest request);
}
