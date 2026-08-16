package com.example.activityservice.feature.complaints.service.impl;

import com.example.activityservice.common.dto.NotificationRequest;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.attendances.model.FaceCheckInAttempt;
import com.example.activityservice.feature.attendances.repository.FaceCheckInAttemptRepository;
import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.dto.ResolveComplaintRequest;
import com.example.activityservice.feature.complaints.mapper.ComplaintMapper;
import com.example.activityservice.feature.complaints.model.Complaints;
import com.example.activityservice.feature.complaints.repository.ComplaintRepository;
import com.example.activityservice.feature.complaints.service.ComplaintService;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintServiceImpl implements ComplaintService {
    private static final int FACE_CHECK_IN_MAX_ATTEMPTS = 5;

    private final ComplaintRepository complaintRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final ComplaintMapper complaintMapper;
    private final FaceCheckInAttemptRepository faceCheckInAttemptRepository;
    private final NotificationCommandProducer notificationCommandProducer;

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
                .map(registration -> enrichFaceAttemptEligibility(
                        complaintMapper.toEligibleResponse(
                                registration,
                                complaintsByRegistrationId.get(registration.getId())),
                        registration))
                .toList();
    }

    @Override
    @Transactional
    public ComplaintResponse submitComplaint(ComplaintRequest request) {
        Users student = getCurrentStudent();
        Registrations registration = registrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Đăng ký không tồn tại"));

        if (registration.getStudent() == null || !registration.getStudent().getId().equals(student.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền khiếu nại đăng ký này");
        }

        validateEligibleRegistration(registration);

        Complaints complaint = complaintRepository.findByRegistrationId(registration.getId()).orElse(null);
        if (complaint == null) {
            complaint = complaintMapper.toNewEntity(request, registration);
        } else {
            if (complaint.getStatus() != null && complaint.getStatus() != 0) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Khiếu nại đã được xử lý, không thể cập nhật");
            }
            complaintMapper.syncRegistrationContext(complaint, registration);
            complaintMapper.updateEntityFromRequest(request, complaint);
            complaint.setStatus(0);
        }

        return complaintMapper.toResponse(complaintRepository.save(complaint));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ComplaintResponse> getComplaints(Long activityId, Integer status, Pageable pageable) {
        Page<Complaints> page;
        if (isAdmin()) {
            page = findComplaintsForAdmin(activityId, status, pageable);
        } else if (isDepartment()) {
            Users reviewer = getCurrentUser();
            Long departmentId = requireDepartmentId(reviewer);
            page = findComplaintsForDepartment(activityId, departmentId, status, pageable);
        } else {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem danh sách khiếu nại");
        }

        return new PageDTO<>(
                page,
                page.getContent().stream()
                        .map(complaintMapper::toResponse)
                        .toList());
    }

    @Override
    @Transactional
    public ComplaintResponse approveComplaint(Long id, ResolveComplaintRequest request) {
        Complaints complaint = resolveReviewableComplaint(id);
        ensureCanReview(complaint);
        ensurePending(complaint);

        String response = normalizeResponse(request);
        complaint.setStatus(1);
        complaint.setResponse(response);
        complaint.setDetailResponse(response);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolvedBy(getCurrentUser());

        Registrations registration = complaint.getRegistration();
        if (registration != null) {
            registration.setStatus(1);
            registrationRepository.save(registration);
        }

        Complaints saved = complaintRepository.save(complaint);
        publishComplaintNotification(
                saved,
                "Khiếu nại điểm danh được duyệt",
                "Khiếu nại của bạn đã được duyệt. Bạn có thể nộp minh chứng tham gia để hệ thống ghi nhận.",
                1,
                "activity-complaint-approved");
        return complaintMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ComplaintResponse rejectComplaint(Long id, ResolveComplaintRequest request) {
        Complaints complaint = resolveReviewableComplaint(id);
        ensureCanReview(complaint);
        ensurePending(complaint);

        String response = normalizeResponse(request);
        complaint.setStatus(2);
        complaint.setResponse(response);
        complaint.setDetailResponse(response);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolvedBy(getCurrentUser());

        Complaints saved = complaintRepository.save(complaint);
        publishComplaintNotification(
                saved,
                "Khiếu nại điểm danh bị từ chối",
                response,
                3,
                "activity-complaint-rejected");
        return complaintMapper.toResponse(saved);
    }

    private Page<Complaints> findComplaintsForAdmin(Long activityId, Integer status, Pageable pageable) {
        if (activityId != null && status != null) {
            return complaintRepository.findByActivityIdAndStatus(activityId, status, pageable);
        }
        if (activityId != null) {
            return complaintRepository.findByActivityId(activityId, pageable);
        }
        if (status != null) {
            return complaintRepository.findByStatus(status, pageable);
        }
        return complaintRepository.findAll(pageable);
    }

    private Page<Complaints> findComplaintsForDepartment(
            Long activityId,
            Long departmentId,
            Integer status,
            Pageable pageable) {
        if (activityId != null && status != null) {
            return complaintRepository.findByActivityIdAndActivityDepartmentIdAndStatus(
                    activityId,
                    departmentId,
                    status,
                    pageable);
        }
        if (activityId != null) {
            return complaintRepository.findByActivityIdAndActivityDepartmentId(activityId, departmentId, pageable);
        }
        if (status != null) {
            return complaintRepository.findByActivityDepartmentIdAndStatus(departmentId, status, pageable);
        }
        return complaintRepository.findByActivityDepartmentId(departmentId, pageable);
    }

    private void validateEligibleRegistration(Registrations registration) {
        if (registration.getStatus() != null && registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Đăng ký đã bị hủy nên không thể khiếu nại");
        }

        boolean attended = registration.getStatus() != null && registration.getStatus() == 1;
        boolean checkedIn = registration.getAttendance() != null && registration.getAttendance().getCheckinTime() != null;
        boolean faceAttemptExhausted = isFaceAttemptExhausted(registration);

        if ((!attended || !checkedIn) && !faceAttemptExhausted) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chỉ có thể khiếu nại hoạt động đã check-in hoặc đã hết 5 lần xác thực khuôn mặt");
        }
    }

    private ComplaintEligibleActivityResponse enrichFaceAttemptEligibility(
            ComplaintEligibleActivityResponse response,
            Registrations registration) {
        long attemptCount = faceCheckInAttemptRepository.countByRegistrationId(registration.getId());
        boolean exhausted = isFaceAttemptExhausted(registration);
        response.setFaceAttemptCount((int) attemptCount);
        response.setFaceAttemptExhausted(exhausted);
        response.setEligibilityReason(exhausted && response.getCheckinTime() == null
                ? "FACE_VERIFICATION_EXHAUSTED"
                : "CHECKED_IN");
        return response;
    }

    private boolean isFaceAttemptExhausted(Registrations registration) {
        long attemptCount = faceCheckInAttemptRepository.countByRegistrationId(registration.getId());
        if (attemptCount < FACE_CHECK_IN_MAX_ATTEMPTS) {
            return false;
        }
        return faceCheckInAttemptRepository.findTopByRegistrationIdOrderByAttemptNoDesc(registration.getId())
                .map(attempt -> !Boolean.TRUE.equals(attempt.getAllowRetry()) && !isSuccessfulFaceAttempt(attempt))
                .orElse(true);
    }

    private boolean isSuccessfulFaceAttempt(FaceCheckInAttempt attempt) {
        if (attempt == null) {
            return false;
        }
        if (Boolean.TRUE.equals(attempt.getVerified())) {
            return true;
        }
        BigDecimal distance = attempt.getDistance();
        BigDecimal threshold = attempt.getThreshold();
        return distance != null && threshold != null && distance.compareTo(threshold) <= 0;
    }

    private Users getCurrentStudent() {
        return getCurrentUser();
    }

    private Users getCurrentUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Complaints resolveReviewableComplaint(Long id) {
        return complaintRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khiếu nại không tồn tại"));
    }

    private void ensureCanReview(Complaints complaint) {
        if (isAdmin()) {
            return;
        }

        Users reviewer = getCurrentUser();
        Activities activity = complaint.getActivity();
        if (isDepartment()
                && activity != null
                && activity.getDepartmentId() != null
                && Objects.equals(activity.getDepartmentId(), requireDepartmentId(reviewer))) {
            return;
        }

        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xử lý khiếu nại này");
    }

    private void ensurePending(Complaints complaint) {
        if (complaint.getStatus() != null && complaint.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khiếu nại đã được xử lý");
        }
    }

    private Long requireDepartmentId(Users user) {
        if (user == null || user.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản chưa được gắn đơn vị");
        }
        return user.getDepartmentId();
    }

    private String normalizeResponse(ResolveComplaintRequest request) {
        String response = request != null ? request.getResponse() : null;
        if (response == null || response.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Nội dung phản hồi không được để trống");
        }
        return response.trim();
    }

    private boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    private boolean isDepartment() {
        return hasRole("ROLE_DEPARTMENT");
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private void publishComplaintNotification(
            Complaints complaint,
            String title,
            String message,
            Integer type,
            String referenceType) {
        Users student = complaint.getStudent() != null
                ? complaint.getStudent()
                : (complaint.getRegistration() != null ? complaint.getRegistration().getStudent() : null);
        Activities activity = complaint.getActivity();
        if (student == null || student.getId() == null) {
            return;
        }

        NotificationRequest notification = new NotificationRequest();
        notification.setUserId(student.getId());
        notification.setActivityId(activity != null ? activity.getId() : null);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setContent(message);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setSourceEventId(referenceType + ":" + complaint.getId());
        notification.setSourceTopic("activity-complaints");

        try {
            notificationCommandProducer.publishCreated(notification);
        } catch (Exception exception) {
            log.warn("Không thể gửi thông báo xử lý khiếu nại id={}: {}", complaint.getId(), exception.getMessage());
        }
    }
}
