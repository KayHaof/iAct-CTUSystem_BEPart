package com.example.activityservice.feature.attendances.service;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;

public interface AttendanceService {
    AttendanceResponse checkIn(CheckInRequest request);
}
