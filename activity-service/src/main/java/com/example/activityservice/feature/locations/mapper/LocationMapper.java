package com.example.activityservice.feature.locations.mapper;

import com.example.activityservice.feature.locations.dto.LocationBookingResponse;
import com.example.activityservice.feature.locations.dto.LocationResponse;
import com.example.activityservice.feature.locations.model.ActivityLocationBooking;
import com.example.activityservice.feature.locations.model.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationResponse toResponse(Location location) {
        if (location == null) {
            return null;
        }
        return LocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .code(location.getCode())
                .type(location.getType())
                .description(location.getDescription())
                .address(location.getAddress())
                .building(location.getBuilding())
                .floor(location.getFloor())
                .room(location.getRoom())
                .capacity(location.getCapacity())
                .managerDepartmentId(location.getManagerDepartmentId())
                .managerUserId(location.getManagerUserId())
                .contactName(location.getContactName())
                .contactPhone(location.getContactPhone())
                .adminManaged(location.getManagerDepartmentId() == null)
                .isBookable(location.getIsBookable())
                .availabilityStatus(location.getAvailabilityStatus())
                .isActive(location.getIsActive())
                .unavailableReason(location.getUnavailableReason())
                .note(location.getNote())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    public LocationBookingResponse toBookingResponse(ActivityLocationBooking booking) {
        if (booking == null) {
            return null;
        }
        Location location = booking.getLocation();
        return LocationBookingResponse.builder()
                .id(booking.getId())
                .activityId(booking.getActivity() == null ? null : booking.getActivity().getId())
                .locationId(location == null ? null : location.getId())
                .scheduleId(booking.getSchedule() == null ? null : booking.getSchedule().getId())
                .scheduleTitle(booking.getSchedule() == null ? null : booking.getSchedule().getTitle())
                .locationName(location == null ? null : location.getName())
                .locationCode(location == null ? null : location.getCode())
                .title(booking.getTitle())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .statusLabel(toStatusLabel(booking.getStatus()))
                .requestedBy(booking.getRequestedBy() == null ? null : booking.getRequestedBy().getId())
                .approvedBy(booking.getApprovedBy() == null ? null : booking.getApprovedBy().getId())
                .approvedAt(booking.getApprovedAt())
                .rejectedReason(booking.getRejectedReason())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private String toStatusLabel(Integer status) {
        if (status == null) {
            return "Không xác định";
        }
        return switch (status) {
            case 0 -> "Chờ duyệt";
            case 1 -> "Đã duyệt";
            case 2 -> "Từ chối";
            case 3 -> "Bản nháp";
            case 4 -> "Đã hủy";
            default -> "Không xác định";
        };
    }
}
