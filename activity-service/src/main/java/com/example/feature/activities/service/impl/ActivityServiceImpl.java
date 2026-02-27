package com.example.feature.activities.service.impl;

import com.example.common.dto.BenefitDto;
import com.example.common.dto.NotificationRequest; // Import DTO
import com.example.common.entity.Benefits;
import com.example.common.entity.Categories;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.common.repository.LocalCategoryRepository;
import com.example.common.repository.LocalSemesterRepository;
import com.example.common.repository.LocalUserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.activities.dto.ActivityApprovalRequest;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.event.ActivityCreatedEvent;
import com.example.feature.activities.mapper.ActivityMapper;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.activities.service.ActivityService;
import com.example.feature.organizers.model.Organizers;
import com.example.feature.organizers.repository.OrganizerRepository;
import com.example.feignClient.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final LocalSemesterRepository semesterRepository;
    private final OrganizerRepository organizerRepository;
    private final LocalCategoryRepository categoryRepository;
    private final LocalUserRepository userRepository;
    private final ActivityMapper activityMapper;

    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;

    // --- CREATE ---
    @Override
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        Semesters semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Semester not found !"));

        Long targetUserId = request.getOrganizerId();

        Organizers organizer = organizerRepository.findById(targetUserId)
                .orElseGet(() -> {
                    Users organizerUser = userRepository.findById(targetUserId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Người phụ trách không tồn tại trong hệ thống User!"));

                    String organizerName = (organizerUser.getFullName() != null && !organizerUser.getFullName().trim().isEmpty())
                            ? organizerUser.getFullName()
                            : organizerUser.getUsername();

                    Organizers newOrganizer = Organizers.builder()
                            .user(organizerUser)
                            .name(organizerName)
                            .build();

                    return organizerRepository.save(newOrganizer);
                });

        Activities activity = activityMapper.toEntity(request, semester, organizer);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String currentUsername = authentication.getName();
            Users currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            activity.setCreatedBy(currentUser);
        }

        // Xử lý Benefits (Quyền lợi)
        List<Benefits> benefitsList = processBenefits(request.getBenefits(), activity);
        if (!benefitsList.isEmpty()) {
            activity.setBenefits(benefitsList);
        }

        Activities savedActivity = activityRepository.save(activity);

        // Event Thông báo
        eventPublisher.publishEvent(new ActivityCreatedEvent(
                savedActivity,
                "Yêu cầu tổ chức hoạt động thành công",
                "Hoạt động '" + savedActivity.getTitle() + "' đã được tạo và đang chờ duyệt.",
                1
        ));

        System.out.println("Hoạt động đã được tạo thành công!");

        return activityMapper.toResponse(savedActivity);
    }

    // --- READ & ALL (Giữ nguyên) ---
    @Override
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !"));
        return activityMapper.toResponse(activity);
    }

    @Override
    public List<ActivityResponse> getAllActivities() {
        return activityRepository.findAll().stream()
                .map(activityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- UPDATE ---
    @Override
    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activities existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !"));

        if (existingActivity.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Only PENDING activities can be modified.");
        }

        activityMapper.updateEntityFromRequest(request, existingActivity);

        if (request.getSemesterId() != null &&
                (existingActivity.getSemester() == null || !existingActivity.getSemester().getId().equals(request.getSemesterId()))) {
            Semesters newSemester = semesterRepository.findById(request.getSemesterId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED));
            existingActivity.setSemester(newSemester);
        }

        if (request.getOrganizerId() != null &&
                (existingActivity.getOrganizer() == null || !existingActivity.getOrganizer().getId().equals(request.getOrganizerId()))) {
            Organizers newOrganizer = organizerRepository.findById(request.getOrganizerId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED));
            existingActivity.setOrganizer(newOrganizer);
        }

        if (request.getBenefits() != null) {
            existingActivity.getBenefits().clear();
            List<Benefits> newBenefits = processBenefits(request.getBenefits(), existingActivity);
            existingActivity.getBenefits().addAll(newBenefits);
        }

        Activities updatedActivity = activityRepository.save(existingActivity);

        // --- GỬI THÔNG BÁO (Update - Type 2: Update) ---
        sendNotificationSafe(updatedActivity,
                "Cập nhật hoạt động",
                "Bạn vừa cập nhật thông tin cho hoạt động '" + updatedActivity.getTitle() + "'.",
                2);

        return activityMapper.toResponse(updatedActivity);
    }

    // --- DELETE ---
    @Override
    @Transactional
    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !");
        }
        activityRepository.deleteById(id);
    }

    // Helper processBenefits
    private List<Benefits> processBenefits(List<BenefitDto> benefitDtos, Activities activity) {
        if (benefitDtos == null) return new java.util.ArrayList<>();
        return benefitDtos.stream().map(dto -> {
            Benefits benefit = activityMapper.toBenefitEntity(dto);
            benefit.setActivity(activity);
            if(dto.getCategoryId() != null) {
                benefit.setCategory(categoryRepository.findById(dto.getCategoryId()).orElse(null));
            }
            return benefit;
        }).collect(Collectors.toList());
    }

    // --- APPROVE ---
    @Override
    @Transactional
    public ActivityResponse approveActivity(Long id, ActivityApprovalRequest request) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found"));

        int currentStatus = activity.getStatus();
        int newStatus = request.getStatus();

        if (currentStatus == 2 || currentStatus == 3) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Cannot change status of a Rejected or Cancelled activity !");
        }

        switch (newStatus) {
            case 1: // APPROVE (Duyệt)
                if (currentStatus != 0) {
                    throw new AppException(ErrorCode.INVALID_ACTION, "Activity is already approved or not in pending status.");
                }
                break;

            case 2: // REJECT (Từ chối)
                if (currentStatus != 0) {
                    throw new AppException(ErrorCode.INVALID_ACTION, "Cannot reject an approved activity. Please use Cancel action.");
                }
                break;

            case 3: // CANCEL (Hủy)
                if (currentStatus != 0 && currentStatus != 1) {
                    throw new AppException(ErrorCode.INVALID_ACTION, "Invalid status for cancellation.");
                }
                break;

            default:
                throw new AppException(ErrorCode.INVALID_KEY, "Invalid status code. Accept: 1 (Approve), 2 (Reject), 3 (Cancel)");
        }

        activity.setStatus(newStatus);
        Activities savedActivity = activityRepository.save(activity);

        // --- GỬI THÔNG BÁO THEO TRẠNG THÁI ---
        if (newStatus == 1) { // APPROVED
            sendNotificationSafe(savedActivity,
                    "Hoạt động đã được duyệt",
                    "Hoạt động '" + savedActivity.getTitle() + "' đã được Admin phê duyệt.",
                    1); // 1 = Info/Success
        }
        else if (newStatus == 2) { // REJECTED
            String reason = request.getRejectReason() != null ? request.getRejectReason() : "Không có lý do";
            sendNotificationSafe(savedActivity,
                    "Hoạt động bị từ chối",
                    "Lý do: " + reason,
                    3); // 3 = Warning/Error
        }
        else if (newStatus == 3) { // CANCELLED
            String reason = request.getCancelReason() != null ? request.getCancelReason() : "Sự cố khẩn cấp";
            sendNotificationSafe(savedActivity,
                    "Hoạt động bị hủy",
                    "Hoạt động '" + savedActivity.getTitle() + "' bị hủy. Lý do: " + reason,
                    3); // 3 = Warning

            // TODO: Ở đây có thể cần logic gửi Broadcast cho sinh viên đã đăng ký
        }

        return activityMapper.toResponse(savedActivity);
    }

    // --- HELPER RIÊNG ĐỂ GỬI THÔNG BÁO ---
    private void sendNotificationSafe(Activities activity, String title, String message, Integer type) {
        try {
            NotificationRequest notiRequest = activityMapper.toNotificationRequest(activity, title, message, type);
            notificationClient.createNotification(notiRequest);
        } catch (Exception e) {
            // Quan trọng: Log lỗi nhưng KHÔNG ném Exception ra ngoài
            // Để tránh việc gửi thông báo lỗi làm Rollback transaction của việc Lưu Activity
            log.error("Failed to send notification for activity {}: {}", activity.getId(), e.getMessage());
        }
    }
}