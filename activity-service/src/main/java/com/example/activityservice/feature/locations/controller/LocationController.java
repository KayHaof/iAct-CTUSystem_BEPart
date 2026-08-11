package com.example.activityservice.feature.locations.controller;

import com.example.activityservice.feature.locations.dto.LocationAvailabilityRequest;
import com.example.activityservice.feature.locations.dto.LocationBookingResponse;
import com.example.activityservice.feature.locations.dto.LocationRequest;
import com.example.activityservice.feature.locations.dto.LocationResponse;
import com.example.activityservice.feature.locations.service.ActivityLocationBookingService;
import com.example.activityservice.feature.locations.service.LocationService;
import com.example.dto.ApiResponse;
import com.example.util.UtcDateTime;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final ActivityLocationBookingService locationBookingService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(@RequestBody LocationRequest request) {
        return new ResponseEntity<>(ApiResponse.success(locationService.createLocation(request)), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getLocations(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean bookable,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long managerDepartmentId,
            @RequestParam(required = false) String availabilityStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean adminManaged) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getLocations(
                active,
                bookable,
                type,
                managerDepartmentId,
                availabilityStatus,
                keyword,
                adminManaged)));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getAvailableLocations(
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long activityId) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getAvailableLocations(
                UtcDateTime.parse(startTime),
                UtcDateTime.parse(endTime),
                minCapacity,
                type,
                keyword,
                activityId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getLocationById(id)));
    }

    @GetMapping("/{id}/bookings")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<List<LocationBookingResponse>>> getLocationBookings(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "month") String view,
            @RequestParam(required = false) List<Integer> statuses) {
        return ResponseEntity.ok(ApiResponse.success(locationBookingService.getLocationSchedule(
                id,
                date,
                view,
                statuses)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @PathVariable Long id,
            @RequestBody LocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(locationService.updateLocation(id, request)));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPARTMENT')")
    public ResponseEntity<ApiResponse<LocationResponse>> updateAvailability(
            @PathVariable Long id,
            @RequestBody LocationAvailabilityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(locationService.updateAvailability(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> activateLocation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(locationService.activateLocation(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocationResponse>> deactivateLocation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(locationService.deactivateLocation(id)));
    }
}
