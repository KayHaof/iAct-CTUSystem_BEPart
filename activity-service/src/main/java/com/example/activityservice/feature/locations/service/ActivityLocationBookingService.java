package com.example.activityservice.feature.locations.service;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.locations.dto.LocationBookingRequest;
import com.example.activityservice.feature.locations.dto.LocationBookingResponse;
import com.example.activityservice.feature.users.model.Users;

import java.time.LocalDate;
import java.util.List;

public interface ActivityLocationBookingService {
    List<LocationBookingResponse> replaceBookings(
            Activities activity,
            List<LocationBookingRequest> requests,
            Users requestedBy,
            Integer initialStatus);
    List<LocationBookingResponse> getBookingsByActivityId(Long activityId);
    List<LocationBookingResponse> getLocationSchedule(
            Long locationId,
            LocalDate date,
            String view,
            List<Integer> statuses);
    void approveBookingsForActivity(Long activityId, Users reviewer);
    void rejectBookingsForActivity(Long activityId, Users reviewer, String reason);
    void cancelBookingsForActivity(Long activityId, Users reviewer, String reason);
}
