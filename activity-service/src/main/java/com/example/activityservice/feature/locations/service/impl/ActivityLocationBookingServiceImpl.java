package com.example.activityservice.feature.locations.service.impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.activityservice.feature.locations.dto.LocationBookingRequest;
import com.example.activityservice.feature.locations.dto.LocationBookingResponse;
import com.example.activityservice.feature.locations.mapper.LocationMapper;
import com.example.activityservice.feature.locations.model.ActivityLocationBooking;
import com.example.activityservice.feature.locations.model.Location;
import com.example.activityservice.feature.locations.repository.ActivityLocationBookingRepository;
import com.example.activityservice.feature.locations.repository.LocationRepository;
import com.example.activityservice.feature.locations.service.ActivityLocationBookingService;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.service.LocalUserResolver;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivityLocationBookingServiceImpl implements ActivityLocationBookingService {

    private static final List<Integer> BLOCKING_STATUSES = List.of(0, 1);
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_DRAFT = 3;
    private static final int STATUS_CANCELLED = 4;
    private final ActivityLocationBookingRepository bookingRepository;
    private final LocationRepository locationRepository;
    private final ActivityScheduleRepository scheduleRepository;
    private final LocationMapper locationMapper;
    private final LocalUserResolver localUserResolver;

    @Override
    @Transactional
    public List<LocationBookingResponse> replaceBookings(
            Activities activity,
            List<LocationBookingRequest> requests,
            Users requestedBy,
            Integer initialStatus) {
        if (activity == null || activity.getId() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khong the dat dia diem khi hoat dong chua ton tai.");
        }

        bookingRepository.deleteByActivityId(activity.getId());
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        validateNoInternalConflicts(requests);
        Integer status = initialStatus == null ? STATUS_PENDING : initialStatus;
        List<ActivityLocationBooking> bookings = requests.stream()
                .sorted(Comparator
                        .comparing((LocationBookingRequest request) -> request.getLocationId())
                        .thenComparing(request -> request.getStartTime()))
                .map(request -> buildBooking(activity, request, requestedBy, status))
                .toList();

        return bookingRepository.saveAll(bookings).stream()
                .map(locationMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationBookingResponse> getBookingsByActivityId(Long activityId) {
        return bookingRepository.findByActivityId(activityId).stream()
                .map(locationMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationBookingResponse> getLocationSchedule(
            Long locationId,
            LocalDate date,
            String view,
            List<Integer> statuses) {
        if (locationId == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long chon dia diem.");
        }
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay dia diem."));
        validateCanViewLocationSchedule(location);
        TimeRange timeRange = resolveScheduleRange(date, view);
        List<Integer> statusFilter = statuses == null || statuses.isEmpty() ? null : statuses;
        return bookingRepository.findScheduleByLocation(
                        locationId,
                        timeRange.startTime(),
                        timeRange.endTime(),
                        statusFilter)
                .stream()
                .map(locationMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional
    public void approveBookingsForActivity(Long activityId, Users reviewer) {
        List<ActivityLocationBooking> bookings = bookingRepository.findByActivityId(activityId);
        for (ActivityLocationBooking booking : bookings) {
            if (STATUS_PENDING == booking.getStatus()) {
                booking.setStatus(STATUS_APPROVED);
                booking.setApprovedBy(reviewer);
                booking.setApprovedAt(LocalDateTime.now());
            }
        }
        bookingRepository.saveAll(bookings);
    }

    @Override
    @Transactional
    public void rejectBookingsForActivity(Long activityId, Users reviewer, String reason) {
        updateBookingsForClosedActivity(activityId, STATUS_REJECTED, reviewer, reason);
    }

    @Override
    @Transactional
    public void cancelBookingsForActivity(Long activityId, Users reviewer, String reason) {
        updateBookingsForClosedActivity(activityId, STATUS_CANCELLED, reviewer, reason);
    }

    private ActivityLocationBooking buildBooking(
            Activities activity,
            LocationBookingRequest request,
            Users requestedBy,
            Integer status) {
        validateRequest(request);
        Location location = locationRepository.findByIdForUpdate(request.getLocationId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay dia diem."));
        ActivitySchedule schedule = resolveSchedule(activity, request);
        validateLocationBookable(location);
        validateCapacity(activity, location);
        if (status != STATUS_DRAFT && status != STATUS_REJECTED && status != STATUS_CANCELLED) {
            validateNoConflict(location.getId(), request.getStartTime(), request.getEndTime(), activity.getId());
        }
        syncScheduleLocation(schedule, location);
        ActivityLocationBooking.ActivityLocationBookingBuilder builder = ActivityLocationBooking.builder()
                .activity(activity)
                .location(location)
                .schedule(schedule)
                .title(trimToNull(request.getTitle()))
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(status)
                .requestedBy(requestedBy);
        if (Integer.valueOf(STATUS_APPROVED).equals(status)) {
            builder.approvedBy(requestedBy)
                    .approvedAt(LocalDateTime.now());
        }
        return builder.build();
    }

    private ActivitySchedule resolveSchedule(Activities activity, LocationBookingRequest request) {
        if (activity == null || activity.getId() == null || request == null) {
            return null;
        }
        if (request.getScheduleId() != null) {
            ActivitySchedule requestedSchedule = scheduleRepository.findById(request.getScheduleId()).orElse(null);
            if (requestedSchedule != null
                    && requestedSchedule.getActivity() != null
                    && Objects.equals(requestedSchedule.getActivity().getId(), activity.getId())) {
                return requestedSchedule;
            }
        }
        List<ActivitySchedule> schedules = activity.getSchedules();
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        List<ActivitySchedule> sameTimeSchedules = schedules.stream()
                .filter(schedule -> Objects.equals(schedule.getStartTime(), request.getStartTime())
                        && Objects.equals(schedule.getEndTime(), request.getEndTime()))
                .toList();
        if (sameTimeSchedules.isEmpty()) {
            return null;
        }
        String requestTitle = trimToNull(request.getTitle());
        if (requestTitle != null) {
            List<ActivitySchedule> sameTitleSchedules = sameTimeSchedules.stream()
                    .filter(schedule -> requestTitle.equals(trimToNull(schedule.getTitle())))
                    .toList();
            if (sameTitleSchedules.size() == 1) {
                return sameTitleSchedules.get(0);
            }
        }
        return sameTimeSchedules.size() == 1 ? sameTimeSchedules.get(0) : null;
    }

    private void syncScheduleLocation(ActivitySchedule schedule, Location location) {
        if (schedule == null || location == null) {
            return;
        }
        schedule.setLocationRef(location);
        schedule.setLocation(location.getName());
    }

    private void updateBookingsForClosedActivity(Long activityId, int status, Users reviewer, String reason) {
        List<ActivityLocationBooking> bookings = bookingRepository.findByActivityId(activityId);
        for (ActivityLocationBooking booking : bookings) {
            if (booking.getStatus() == STATUS_PENDING || booking.getStatus() == STATUS_APPROVED) {
                booking.setStatus(status);
                booking.setApprovedBy(reviewer);
                booking.setApprovedAt(LocalDateTime.now());
                booking.setRejectedReason(reason);
            }
        }
        bookingRepository.saveAll(bookings);
    }

    private void validateRequest(LocationBookingRequest request) {
        if (request == null || request.getLocationId() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long chon dia diem.");
        }
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khung thoi gian dat dia diem khong hop le.");
        }
    }

    private void validateNoInternalConflicts(List<LocationBookingRequest> requests) {
        List<LocationBookingRequest> normalizedRequests = new ArrayList<>();
        for (LocationBookingRequest request : requests) {
            validateRequest(request);
            normalizedRequests.add(request);
        }
        for (int i = 0; i < normalizedRequests.size(); i++) {
            LocationBookingRequest current = normalizedRequests.get(i);
            for (int j = i + 1; j < normalizedRequests.size(); j++) {
                LocationBookingRequest other = normalizedRequests.get(j);
                if (current.getLocationId().equals(other.getLocationId())
                        && current.getStartTime().isBefore(other.getEndTime())
                        && current.getEndTime().isAfter(other.getStartTime())) {
                    throw new AppException(ErrorCode.INVALID_ACTION,
                            "Cac khung gio dat cung mot dia diem trong hoat dong dang bi trung nhau.");
                }
            }
        }
    }

    private void validateLocationBookable(Location location) {
        if (!Boolean.TRUE.equals(location.getIsActive())
                || !Boolean.TRUE.equals(location.getIsBookable())
                || !"AVAILABLE".equalsIgnoreCase(location.getAvailabilityStatus())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Dia diem dang khong san sang cho muon: " + location.getName());
        }
    }

    private void validateCanViewLocationSchedule(Location location) {
        if (hasCurrentRole("ROLE_ADMIN")) {
            return;
        }
        if (hasCurrentRole("ROLE_DEPARTMENT")) {
            Long departmentId = requireCurrentDepartmentId();
            if (Objects.equals(departmentId, location.getManagerDepartmentId())) {
                return;
            }
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen xem lich su dung dia diem nay.");
    }

    private Long requireCurrentDepartmentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tai khoan chua duoc gan don vi quan ly dia diem.");
        }
        Users currentUser = localUserResolver.resolveByUsername(authentication.getName());
        if (currentUser.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tai khoan chua duoc gan don vi quan ly dia diem.");
        }
        return currentUser.getDepartmentId();
    }

    private boolean hasCurrentRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private void validateCapacity(Activities activity, Location location) {
        if (activity.getMaxParticipants() != null
                && location.getCapacity() != null
                && activity.getMaxParticipants() > location.getCapacity()) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Suc chua dia diem khong du cho so luong toi da cua hoat dong: " + location.getName());
        }
    }

    private void validateNoConflict(
            Long locationId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long activityId) {
        List<ActivityLocationBooking> conflicts = bookingRepository.findBlockingBookingsForUpdate(
                locationId,
                startTime,
                endTime,
                BLOCKING_STATUSES,
                activityId);
        if (!conflicts.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Dia diem da co lich dang giu cho hoac da duyet trong khung thoi gian nay.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TimeRange resolveScheduleRange(LocalDate date, String view) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        String normalizedView = view == null || view.isBlank() ? "month" : view.trim().toLowerCase();
        return switch (normalizedView) {
            case "day" -> new TimeRange(targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
            case "week" -> {
                LocalDate startOfWeek = targetDate.with(DayOfWeek.MONDAY);
                yield new TimeRange(startOfWeek.atStartOfDay(), startOfWeek.plusWeeks(1).atStartOfDay());
            }
            case "month" -> {
                LocalDate startOfMonth = targetDate.withDayOfMonth(1);
                yield new TimeRange(startOfMonth.atStartOfDay(), startOfMonth.plusMonths(1).atStartOfDay());
            }
            default -> throw new AppException(ErrorCode.INVALID_ACTION,
                    "Kieu lich khong hop le. Chi ho tro day, week hoac month.");
        };
    }

    private record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
