package com.example.feature.activities.service.impl;

import com.example.common.dto.BenefitDto;
import com.example.common.dto.NotificationRequest; // Import DTO
import com.example.common.entity.Benefits;
import com.example.common.entity.Categories;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.common.repository.LocalCategoryRepository;
import com.example.common.repository.LocalNotificationRepository;
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
import com.example.feature.registration.repository.RegistrationRepository;
import com.example.feignClient.NotificationClient;
import com.example.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final LocalNotificationRepository notificationRepository;
    private final LocalUserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityMapper activityMapper;

    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;

    private final CloudinaryService cloudinaryService;

    // --- CREATE ---
    @Override
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        // 1. Xử lý Semester
        Semesters semester = null;
        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            semester = semesterRepository.findSemesterByDate(activityDate)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày bắt đầu tổ chức (" + activityDate + ") không thuộc bất kỳ Học kỳ nào đang được cấu hình!"));
        } else {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn Ngày bắt đầu tổ chức!");
        }

        // 2. Xử lý Organizer
        Organizers organizer = null;
        if (request.getOrganizerId() != null) {
            Long userId = request.getOrganizerId();

            // Bước A: Tìm User trong hệ thống trước (Bắt buộc phải có User mới làm Organizer được)
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy User với ID: " + userId));

            // Bước B: Tìm trong bảng organizers xem User này đã được đăng ký làm Organizer chưa
            // Vì user_id là PK của bảng organizers nên findById(userId) là chuẩn bài
            organizer = organizerRepository.findById(userId)
                    .orElseGet(() -> {
                        // Nếu chưa có thì "gán" User này vào bảng organizers
                        log.info("User {} lần đầu tổ chức hoạt động, đang tạo bản ghi Organizer...", user.getUsername());

                        String displayName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                                ? user.getFullName()
                                : user.getUsername();

                        Organizers newOrg = Organizers.builder()
                                .user(user) // Link tới thực thể Users
                                .name(displayName)
                                .build();

                        return organizerRepository.save(newOrg);
                    });
        }

        // 3. Map sang Entity
        Activities activity = activityMapper.toEntity(request, semester, organizer);

        // 4. Set người tạo hoạt động
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String currentUsername = authentication.getName();
            Users currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            activity.setCreatedBy(currentUser);
            activity.setCreatedByUsername(currentUsername);
        }

        // 5. Xử lý Benefits (Bọc check null an toàn)
        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
            List<Benefits> benefitsList = processBenefits(request.getBenefits(), activity);
            if (!benefitsList.isEmpty()) {
                activity.setBenefits(benefitsList);
            }
        }

        // 6. Lưu xuống DB
        Activities savedActivity = activityRepository.save(activity);

        // 7. Event Thông báo (CHỈ gửi khi Gửi duyệt - status = 0, KHÔNG gửi khi Lưu nháp - status = 3)
        if (savedActivity.getStatus() != 3) {
            eventPublisher.publishEvent(new ActivityCreatedEvent(
                    savedActivity,
                    "Yêu cầu tổ chức hoạt động thành công",
                    "Hoạt động '" + savedActivity.getTitle() + "' đã được tạo và đang chờ duyệt.",
                    1
            ));
            System.out.println("Hoạt động đã được tạo và gửi duyệt thành công!");
        } else {
            System.out.println("Bản nháp hoạt động đã được lưu thành công!");
        }

        return activityMapper.toResponse(savedActivity);
    }

    // --- READ & ALL ---
    @Override
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động"));

        ActivityResponse response = activityMapper.toResponse(activity);
        long count = registrationRepository.countByActivityIdAndStatusNot(id, 2);
        response.setRegisteredCount((int) count);

        return response;
    }

    @Override
    public List<ActivityResponse> getAllActivities(Integer status) {
        List<Activities> activities;

        if (status != null) {
            activities = activityRepository.findByStatus(status);
        } else {
            activities = activityRepository.findAll();
        }

        return activities.stream()
                .map(activity -> {
                    // 1. Map Entity sang DTO
                    ActivityResponse response = activityMapper.toResponse(activity);

                    // 2. Đếm số lượng thực tế cho từng hoạt động (Bỏ qua status 2 = Đã hủy)
                    long count = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);

                    // 3. Gắn con số vào Response
                    response.setRegisteredCount((int) count);

                    return response;
                })
                .collect(Collectors.toList());
    }

    // --- UPDATE ---
    @Override
    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activities existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));

        // Chỉ cho phép sửa khi PENDING (0) hoặc DRAFT (3)
        if (existingActivity.getStatus() != 0 && existingActivity.getStatus() != 3) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ có thể chỉnh sửa hoạt động đang chờ duyệt hoặc bản nháp.");
        }

        String oldCoverImg = existingActivity.getCoverImage();
        String oldThumbnailImg = existingActivity.getThumbnail();

        // 1. Map data cơ bản từ Request đè lên Entity (nhờ MapStruct)
        activityMapper.updateEntityFromRequest(request, existingActivity);

        // Xóa ảnh cũ trên Cloudinary
        if(request.getCoverImage() != null && !request.getCoverImage().equals(oldCoverImg)){
            deleteOldImage(oldCoverImg);
        }
        if(request.getThumbnail() != null && !request.getThumbnail().equals(oldThumbnailImg)){
            deleteOldImage(oldThumbnailImg);
        }

        // =========================================================
        // 2. XỬ LÝ SEMESTER TỰ ĐỘNG THEO NGÀY (Nâng cấp giống Create)
        // =========================================================
        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            Semesters newSemester = semesterRepository.findSemesterByDate(activityDate)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày bắt đầu tổ chức (" + activityDate + ") không thuộc bất kỳ Học kỳ nào!"));
            existingActivity.setSemester(newSemester);
        }

        // =========================================================
        // 3. XỬ LÝ ORGANIZER THÔNG MINH (Chèn mới nếu chưa có)
        // =========================================================
        if (request.getOrganizerId() != null &&
                (existingActivity.getOrganizer() == null || !existingActivity.getOrganizer().getId().equals(request.getOrganizerId()))) {

            Long userId = request.getOrganizerId();

            // Bước A: Tìm xem User có tồn tại không
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy User với ID: " + userId));

            // Bước B: Dò trong bảng Organizers, không có thì chèn vào
            Organizers newOrganizer = organizerRepository.findById(userId)
                    .orElseGet(() -> {
                        log.info("Cập nhật hoạt động: Đang tự động thêm User {} vào bảng Organizers...", user.getUsername());

                        String displayName = (user.getFullName() != null && !user.getFullName().trim().isEmpty())
                                ? user.getFullName()
                                : user.getUsername();

                        Organizers newOrg = Organizers.builder()
                                .user(user)
                                .name(displayName)
                                .build();

                        return organizerRepository.save(newOrg);
                    });

            existingActivity.setOrganizer(newOrganizer);
        }

        // 4. Xử lý Benefits (Điểm rèn luyện)
        if (request.getBenefits() != null) {
            existingActivity.getBenefits().clear(); // Xóa sạch data cũ của hoạt động này
            List<Benefits> newBenefits = processBenefits(request.getBenefits(), existingActivity); // Áp data mới
            existingActivity.getBenefits().addAll(newBenefits);
        }

        // 5. Lưu xuống Database
        Activities updatedActivity = activityRepository.save(existingActivity);

        // 6. GỬI THÔNG BÁO
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
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !"));

        Integer currentStatus = activity.getStatus();

        if (currentStatus != null && (currentStatus == 1 || currentStatus == 4)) {
            throw new AppException(
                    ErrorCode.INVALID_ACTION,
                    "Không thể xóa hoạt động đã được phê duyệt hoặc đã hủy! Chỉ có thể xóa bản nháp, chờ duyệt hoặc bị từ chối."
            );
        }

       if (notificationRepository.existsByActivityId(id)) {
            notificationRepository.deleteByActivityId(id);
       }

        deleteOldImage(activity.getCoverImage());
        deleteOldImage(activity.getThumbnail());

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

            case 4: // CANCEL (Hủy)
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

    // Helper functions
    public void deleteOldImage(String oldImg) {
        cloudinaryService.deleteImageByUrl(oldImg);
    }
}