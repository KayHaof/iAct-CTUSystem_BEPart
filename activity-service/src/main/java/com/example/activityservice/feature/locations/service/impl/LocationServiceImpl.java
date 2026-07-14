package com.example.activityservice.feature.locations.service.impl;

import com.example.activityservice.feature.locations.dto.LocationAvailabilityRequest;
import com.example.activityservice.feature.locations.dto.LocationRequest;
import com.example.activityservice.feature.locations.dto.LocationResponse;
import com.example.activityservice.feature.locations.mapper.LocationMapper;
import com.example.activityservice.feature.locations.model.Location;
import com.example.activityservice.feature.locations.repository.ActivityLocationBookingRepository;
import com.example.activityservice.feature.locations.repository.LocationRepository;
import com.example.activityservice.feature.locations.service.LocationService;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final List<Integer> BLOCKING_STATUSES = List.of(0, 1);
    private static final String AVAILABLE = "AVAILABLE";

    private final LocationRepository locationRepository;
    private final ActivityLocationBookingRepository bookingRepository;
    private final LocationMapper locationMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        validateAdminPermission();
        validateRequired(request);
        validateManagerScope(request);
        validateCodeUnique(request.getCode(), null);
        Location location = new Location();
        applyRequest(location, request, true);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocations(
            Boolean active,
            Boolean bookable,
            String type,
            Long managerDepartmentId,
            String availabilityStatus,
            String keyword,
            Boolean adminManaged) {
        return locationRepository.findAll().stream()
                .filter(location -> active == null || active.equals(location.getIsActive()))
                .filter(location -> bookable == null || bookable.equals(location.getIsBookable()))
                .filter(location -> type == null || type.isBlank() || type.equalsIgnoreCase(location.getType()))
                .filter(location -> managerDepartmentId == null
                        || managerDepartmentId.equals(location.getManagerDepartmentId()))
                .filter(location -> availabilityStatus == null
                        || availabilityStatus.isBlank()
                        || availabilityStatus.equalsIgnoreCase(location.getAvailabilityStatus()))
                .filter(location -> adminManaged == null
                        || adminManaged.equals(location.getManagerDepartmentId() == null))
                .filter(location -> matchesKeyword(location, keyword))
                .map(locationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAvailableLocations(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer minCapacity,
            String type,
            Long managerDepartmentId,
            String keyword,
            Boolean adminManaged) {
        validateTimeRange(startTime, endTime);
        return locationRepository.findByIsActiveTrueAndIsBookableTrueAndAvailabilityStatus(AVAILABLE).stream()
                .filter(location -> type == null || type.isBlank() || type.equalsIgnoreCase(location.getType()))
                .filter(location -> managerDepartmentId == null
                        || managerDepartmentId.equals(location.getManagerDepartmentId()))
                .filter(location -> adminManaged == null
                        || adminManaged.equals(location.getManagerDepartmentId() == null))
                .filter(location -> minCapacity == null
                        || location.getCapacity() == null
                        || location.getCapacity() >= minCapacity)
                .filter(location -> matchesKeyword(location, keyword))
                .filter(location -> bookingRepository.countConflicts(
                        location.getId(),
                        startTime,
                        endTime,
                        BLOCKING_STATUSES) == 0)
                .map(locationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long id) {
        return locationMapper.toResponse(findLocationOrThrow(id));
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(Long id, LocationRequest request) {
        validateAdminPermission();
        validateRequired(request);
        validateManagerScope(request);
        Location location = findLocationOrThrow(id);
        validateManagePermission(location);
        validateCodeUnique(request.getCode(), id);
        applyRequest(location, request, hasCurrentRole("ROLE_ADMIN"));
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponse updateAvailability(Long id, LocationAvailabilityRequest request) {
        Location location = findLocationOrThrow(id);
        validateManagePermission(location);
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long cung cap trang thai kha dung.");
        }
        if (request.getIsBookable() != null) {
            location.setIsBookable(request.getIsBookable());
        }
        if (request.getAvailabilityStatus() != null && !request.getAvailabilityStatus().isBlank()) {
            location.setAvailabilityStatus(normalizeAvailabilityStatus(request.getAvailabilityStatus()));
        }
        location.setUnavailableReason(trimToNull(request.getUnavailableReason()));
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponse activateLocation(Long id) {
        validateAdminPermission();
        Location location = findLocationOrThrow(id);
        validateManagePermission(location);
        location.setIsActive(true);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Override
    @Transactional
    public LocationResponse deactivateLocation(Long id) {
        validateAdminPermission();
        Location location = findLocationOrThrow(id);
        validateManagePermission(location);
        location.setIsActive(false);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    private void applyRequest(Location location, LocationRequest request, boolean canChangeOwnership) {
        location.setName(request.getName().trim());
        location.setCode(normalizeCode(request.getCode()));
        location.setType(request.getType().trim().toUpperCase());
        location.setDescription(trimToNull(request.getDescription()));
        location.setAddress(trimToNull(request.getAddress()));
        location.setBuilding(trimToNull(request.getBuilding()));
        location.setFloor(trimToNull(request.getFloor()));
        location.setRoom(trimToNull(request.getRoom()));
        location.setCapacity(request.getCapacity());
        if (canChangeOwnership) {
            location.setManagerDepartmentId(Boolean.TRUE.equals(request.getAdminManaged())
                    ? null
                    : request.getManagerDepartmentId());
            location.setManagerUserId(Boolean.TRUE.equals(request.getAdminManaged())
                    ? null
                    : request.getManagerUserId());
        }
        location.setContactName(trimToNull(request.getContactName()));
        location.setContactPhone(trimToNull(request.getContactPhone()));
        location.setIsBookable(request.getIsBookable() == null ? Boolean.TRUE : request.getIsBookable());
        location.setAvailabilityStatus(normalizeAvailabilityStatus(request.getAvailabilityStatus()));
        location.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        location.setUnavailableReason(trimToNull(request.getUnavailableReason()));
        location.setNote(trimToNull(request.getNote()));
    }

    private void validateRequired(LocationRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long nhap ten dia diem.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long chon loai dia diem.");
        }
        if (request.getCapacity() != null && request.getCapacity() < 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Suc chua dia diem khong hop le.");
        }
    }

    private void validateManagerScope(LocationRequest request) {
        if (request == null) {
            return;
        }
        if (Boolean.TRUE.equals(request.getAdminManaged())) {
            return;
        }
        if (request.getManagerDepartmentId() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Vui long chon khoa/truong quan ly dia diem hoac danh dau dia diem do admin quan ly.");
        }
    }

    private void validateCodeUnique(String rawCode, Long currentId) {
        String code = normalizeCode(rawCode);
        if (code == null) {
            return;
        }
        boolean existed = currentId == null
                ? locationRepository.existsByCode(code)
                : locationRepository.existsByCodeAndIdNot(code, currentId);
        if (existed) {
            throw new AppException(ErrorCode.INVALID_KEY, "Ma dia diem da ton tai.");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khung thoi gian dia diem khong hop le.");
        }
    }

    private Location findLocationOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay dia diem."));
    }

    private boolean matchesKeyword(Location location, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        return containsIgnoreCase(location.getName(), normalizedKeyword)
                || containsIgnoreCase(location.getCode(), normalizedKeyword)
                || containsIgnoreCase(location.getBuilding(), normalizedKeyword)
                || containsIgnoreCase(location.getRoom(), normalizedKeyword)
                || containsIgnoreCase(location.getAddress(), normalizedKeyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase().contains(normalizedKeyword);
    }

    private void validateManagePermission(Location location) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen cap nhat dia diem nay.");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        boolean isDepartment = authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), "ROLE_DEPARTMENT"));
        if (!isDepartment) {
            throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen cap nhat dia diem nay.");
        }
        Users currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null
                || currentUser.getDepartmentId() == null
                || !Objects.equals(currentUser.getDepartmentId(), location.getManagerDepartmentId())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Don vi chi duoc cap nhat dia diem do khoa/truong cua minh quan ly.");
        }
    }

    private void validateAdminPermission() {
        if (!hasCurrentRole("ROLE_ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chi admin moi duoc thuc hien thao tac nay.");
        }
    }

    private boolean hasCurrentRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), role));
    }

    private String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase();
    }

    private String normalizeAvailabilityStatus(String status) {
        return status == null || status.isBlank() ? AVAILABLE : status.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
