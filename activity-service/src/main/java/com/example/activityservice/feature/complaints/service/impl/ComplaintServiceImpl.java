package com.example.activityservice.feature.complaints.service.impl;

import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.mapper.ComplaintMapper;
import com.example.activityservice.feature.complaints.model.Complaints;
import com.example.activityservice.feature.complaints.repository.ComplaintRepository;
import com.example.activityservice.feature.complaints.service.ComplaintService;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final ComplaintMapper complaintMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintEligibleActivityResponse> getMyEligibleActivities(Long semesterId) {
        Users student = getCurrentStudent();
        List<Registrations> registrations = registrationRepository.findComplaintEligibleRegistrations(
                student.getId(),
                semesterId);

        List<Long> registrationIds = registrations.stream()
                .map(registration -> registration.getId())
                .toList();

        if (registrationIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Complaints> complaintsByRegistrationId = complaintRepository
                .findByRegistrationIdIn(registrationIds)
                .stream()
                .collect(Collectors.toMap(
                        complaint -> complaint.getRegistration().getId(),
                        Function.identity()));

        return registrations.stream()
                .map(registration -> complaintMapper.toEligibleResponse(
                        registration,
                        complaintsByRegistrationId.get(registration.getId())))
                .toList();
    }

    @Override
    @Transactional
    public ComplaintResponse submitComplaint(ComplaintRequest request) {
        Users student = getCurrentStudent();
        Registrations registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Dang ky khong ton tai"));

        if (registration.getStudent() == null || !registration.getStudent().getId().equals(student.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Ban khong co quyen khieu nai dang ky nay");
        }

        validateEligibleRegistration(registration);

        Complaints complaint = complaintRepository.findByRegistrationId(registration.getId()).orElse(null);
        if (complaint == null) {
            complaint = complaintMapper.toNewEntity(request, registration);
        } else {
            if (complaint.getStatus() != null && complaint.getStatus() != 0) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Khieu nai da duoc xu ly, khong the cap nhat");
            }
            complaintMapper.updateEntityFromRequest(request, complaint);
            complaint.setStatus(0);
        }

        return complaintMapper.toResponse(complaintRepository.save(complaint));
    }

    private void validateEligibleRegistration(Registrations registration) {
        boolean attended = registration.getStatus() != null && registration.getStatus() == 1;
        boolean checkedIn = registration.getAttendance() != null && registration.getAttendance().getCheckinTime() != null;
        boolean checkedOut = registration.getAttendance() != null && registration.getAttendance().getCheckoutTime() != null;

        if (!attended || !checkedIn || !checkedOut) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chi co the khieu nai hoat dong da tham gia day du");
        }
    }

    private Users getCurrentStudent() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
