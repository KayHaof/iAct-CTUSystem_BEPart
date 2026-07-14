package com.example.activityservice.feature.locations.service;

import com.example.activityservice.feature.locations.dto.LocationAvailabilityRequest;
import com.example.activityservice.feature.locations.dto.LocationRequest;
import com.example.activityservice.feature.locations.dto.LocationResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface LocationService {
    LocationResponse createLocation(LocationRequest request);
    List<LocationResponse> getLocations(
            Boolean active,
            Boolean bookable,
            String type,
            Long managerDepartmentId,
            String availabilityStatus,
            String keyword,
            Boolean adminManaged);
    List<LocationResponse> getAvailableLocations(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer minCapacity,
            String type,
            Long managerDepartmentId,
            String keyword,
            Boolean adminManaged);
    LocationResponse getLocationById(Long id);
    LocationResponse updateLocation(Long id, LocationRequest request);
    LocationResponse updateAvailability(Long id, LocationAvailabilityRequest request);
    LocationResponse activateLocation(Long id);
    LocationResponse deactivateLocation(Long id);
}
