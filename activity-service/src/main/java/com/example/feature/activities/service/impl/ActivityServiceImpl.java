package com.example.feature.activities.service.impl;

import com.example.common.dto.NotificationRequest;
import com.example.common.dto.ProfileDto;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.common.repository.LocalNotificationRepository;
import com.example.common.repository.LocalSemesterRepository;
import com.example.common.repository.LocalUserRepository;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.activities.dto.ActivityRequest;
import com.example.feature.activities.dto.ActivityResponse;
import com.example.feature.activities.dto.ActivityStatsResponse;
import com.example.feature.activities.dto.ActivityTimeLocationResponse;
import com.example.feature.activities.event.ActivityCreatedEvent;
import com.example.feature.activities.mapper.ActivityMapper;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.activities.service.ActivityService;
import com.example.feature.activities.specification.ActivitySpecification;
import com.example.feature.activitySchedule.mapper.ActivityScheduleMapper;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.organizers.model.Organizers;
import com.example.feature.organizers.repository.OrganizerRepository;
import com.example.feature.registration.repository.RegistrationRepository;
import com.example.feignClient.NotificationClient;
import com.example.feignClient.ProfileClient;
import com.example.service.CloudinaryService;
import com.example.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final LocalSemesterRepository semesterRepository;
    private final OrganizerRepository organizerRepository;
    private final LocalNotificationRepository notificationRepository;
    private final LocalUserRepository userRepository;
    private final RegistrationRepository registrationRepository;

    private final ActivityMapper activityMapper;
    private final ActivityScheduleMapper scheduleMapper;

    private final NotificationClient notificationClient;
    private final ProfileClient profileClient;
    private final ApplicationEventPublisher eventPublisher;

    private final CloudinaryService cloudinaryService;
    private final QRCodeService qrCodeService;

    private ProfileDto getProfileData(Users user) {
        try {
            ApiResponse<ProfileDto> profileRes = profileClient.getProfile(user.getId());
            if (profileRes != null && profileRes.getResult() != null) {
                log.info("<< Gọi FeignClient thành công: {}", profileRes.getResult());
                return profileRes.getResult();
            }
        } catch (Exception e) {
            log.warn("Lỗi gọi FeignClient lấy Profile cho user {}: {}", user.getId(), e.getMessage());
        }
        return null;
    }

    // --- CREATE ---
    @Override
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        Semesters semester;
        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            semester = semesterRepository.findSemesterByDate(activityDate)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày bắt đầu tổ chức (" + activityDate + ") không thuộc bất kỳ Học kỳ nào đang được cấu hình!"));
        } else {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn Ngày bắt đầu tổ chức!");
        }

        Organizers organizer = null;
        if (request.getOrganizerId() != null) {
            Long userId = request.getOrganizerId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy User với ID: " + userId));

            organizer = getOrCreateOrganizer(user);
        }

        // [FIX LỖI 117] Bỏ truyền `semester` vào mapper (do mapper đã bỏ). Tự map ID thủ công
        Activities activity = activityMapper.toEntity(request, organizer);
        activity.setSemesterId(semester.getId());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String currentUsername = authentication.getName();
            Users currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // [FIX LỖI 124] Truyền Long ID thay vì Object Users
            activity.setCreatedBy(currentUser.getId());
            activity.setCreatedByUsername(currentUsername);
            activity.setDepartmentId(Objects.requireNonNull(getProfileData(currentUser)).getDepartmentId());
        }

        // [FIX LỖI 132] Tạm thời KHÔNG lưu trực tiếp Benefits. Giao phó cho Kafka
        if (request.getBenefits() != null && !request.getBenefits().isEmpty()) {
            log.info("TODO KAFKA: Sẽ gửi danh sách Benefits qua Credit Service qua topic 'activity-created'");
            // Code bắn Kafka sẽ nằm ở đây sau này.
        }

        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            List<ActivitySchedule> schedulesList = scheduleMapper.toEntityList(request.getSchedules());
            schedulesList.forEach(schedule -> schedule.setActivity(activity));
            activity.setSchedules(schedulesList);
        }

        Activities savedActivity = activityRepository.save(activity);

        if (savedActivity.getStatus() != 3) {
            eventPublisher.publishEvent(new ActivityCreatedEvent(
                    savedActivity,
                    "Yêu cầu tổ chức hoạt động thành công",
                    "Hoạt động '" + savedActivity.getTitle() + "' đã được tạo và đang chờ duyệt.",
                    1
            ));
            log.info("Hoạt động đã được tạo và gửi duyệt thành công!");
        } else {
            log.info("Bản nháp hoạt động đã được lưu thành công!");
        }

        return activityMapper.toResponse(savedActivity);
    }

    // --- READ DETAILS ---
    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động"));

        ActivityResponse response = activityMapper.toResponse(activity);
        long count = registrationRepository.countByActivityIdAndStatusNot(id, 2);
        response.setRegisteredCount((int) count);
        return response;
    }

    // GET TIMES AND LOCATION OF ACT
    @Override
    public ActivityTimeLocationResponse getActivityTimesAndLocation(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động với ID: " + id));
        return activityMapper.toTimeResponse(activity);
    }

    // --- READ ALL ---
    @Override
    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Long departmentId, Pageable pageable) {
        Long userDeptId = null;

        boolean isAdmin = false;
        boolean isDepartment = false;
        boolean isStudent = true;

        Users currentUser = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            isAdmin = authentication.getAuthorities().stream().anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
            isDepartment = authentication.getAuthorities().stream().anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_DEPARTMENT"));
            isStudent = !isAdmin && !isDepartment;

            String username = authentication.getName();
            Optional<Users> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                if (isAdmin || isDepartment) {
                    Optional<Organizers> orgOpt = organizerRepository.findById(currentUser.getId());
                    // [FIX LỖI 203, 204] Đổi từ getDepartment() thành getDepartmentId()
                    if (orgOpt.isPresent() && orgOpt.get().getDepartmentId() != null) {
                        userDeptId = orgOpt.get().getDepartmentId();
                    }
                } else {
                    try {
                        ApiResponse<ProfileDto> profileRes = profileClient.getProfile(currentUser.getId());
                        if (profileRes != null && profileRes.getResult() != null) {
                            userDeptId = profileRes.getResult().getDepartmentId();
                        }
                    } catch (Exception e) {
                        log.warn("Không lấy được thông tin Khoa của Sinh viên {}", currentUser.getId());
                    }
                }
            }
        }

        Specification<Activities> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (isStudent) {
            spec = spec.and(ActivitySpecification.isApproved());
        } else if (isDepartment && currentUser != null) {
            spec = spec.and(ActivitySpecification.isOwnedByOrOrganizedBy(currentUser.getId()));
        } else if (isAdmin) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.notEqual(root.get("status"), 3)
            );
        }

        if (departmentId != null) {
            spec = spec.and(ActivitySpecification.hasDepartmentId(departmentId));
        }

        boolean isOrganizer = isAdmin || isDepartment;
        spec = spec.and(ActivitySpecification.containsKeyword(keyword))
                .and(ActivitySpecification.hasLevel(level, userDeptId))
                .and(ActivitySpecification.hasStatus(status, keyword, isOrganizer));

        Page<Activities> pageActivities = activityRepository.findAll(spec, pageable);

        List<ActivityResponse> dtoList = pageActivities.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = activityMapper.toResponse(activity);
                    long count = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);
                    response.setRegisteredCount((int) count);
                    return response;
                })
                .collect(Collectors.toList());

        return activityMapper.toPageDTO(pageActivities, dtoList);
    }

    // --- UPDATE ---
    @Override
    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activities existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));

        if (existingActivity.getStatus() != 0 && existingActivity.getStatus() != 3) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ có thể chỉnh sửa hoạt động đang chờ duyệt hoặc bản nháp.");
        }

        String oldCoverImg = existingActivity.getCoverImage();
        String oldThumbnailImg = existingActivity.getThumbnail();

        activityMapper.updateEntityFromRequest(request, existingActivity);

        if(request.getCoverImage() != null && !request.getCoverImage().equals(oldCoverImg)){
            deleteOldImage(oldCoverImg);
        }
        if(request.getThumbnail() != null && !request.getThumbnail().equals(oldThumbnailImg)){
            deleteOldImage(oldThumbnailImg);
        }

        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            Semesters newSemester = semesterRepository.findSemesterByDate(activityDate)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày bắt đầu tổ chức (" + activityDate + ") không thuộc bất kỳ Học kỳ nào!"));
            // [FIX LỖI 282]
            existingActivity.setSemesterId(newSemester.getId());
        }

        if (request.getOrganizerId() != null &&
                (existingActivity.getOrganizer() == null || !existingActivity.getOrganizer().getId().equals(request.getOrganizerId()))) {

            Long userId = request.getOrganizerId();

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy User với ID: " + userId));

            Organizers newOrganizer = getOrCreateOrganizer(user);
            existingActivity.setOrganizer(newOrganizer);
        }

        // [FIX LỖI 298, 300] Comment lại việc set Benefits. Dùng Kafka sau
        if (request.getBenefits() != null) {
            log.info("TODO KAFKA: Gửi lệnh cập nhật Benefits qua Credit Service (Topic: activity-updated)");
            /*
            existingActivity.getBenefits().clear();
            List<Benefits> newBenefits = processBenefits(request.getBenefits(), existingActivity);
            existingActivity.getBenefits().addAll(newBenefits);
            */
        }

        if (request.getSchedules() != null) {
            if (existingActivity.getSchedules() != null) {
                existingActivity.getSchedules().clear();
            } else {
                existingActivity.setSchedules(new ArrayList<>());
            }

            List<ActivitySchedule> newSchedules = scheduleMapper.toEntityList(request.getSchedules());
            newSchedules.forEach(schedule -> schedule.setActivity(existingActivity));

            existingActivity.getSchedules().addAll(newSchedules);
        }

        Activities updatedActivity = activityRepository.save(existingActivity);

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
        log.info("TODO KAFKA: Gửi lệnh xóa Benefits qua Credit Service (Topic: activity-deleted)");
    }

    // [FIX LỖI 355] Đã xóa (hoặc comment) hàm processBenefits vì ta không còn dùng Entity Benefits ở đây nữa.

    // --- APPROVE ---
    @Override
    @Transactional
    public void approveActivity(Long id) {
        Activities activity = getActivityForAction(id);

        if (activity.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ có thể duyệt hoạt động đang ở trạng thái Chờ duyệt.");
        }

        activity.setStatus(1);
        // [FIX LỖI 375] set HandledBy bằng ID
        Users admin = getCurrentAdmin();
        activity.setHandledBy(admin != null ? admin.getId() : null);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);

        sendNotificationSafe(savedActivity, "Hoạt động đã được duyệt",
                "Hoạt động '" + savedActivity.getTitle() + "' đã được phê duyệt thành công.", 1);
    }

    // --- REJECT ---
    @Override
    @Transactional
    public void rejectActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);

        if (activity.getStatus() != 0) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ có thể từ chối hoạt động đang ở trạng thái Chờ duyệt.");
        }

        activity.setStatus(2);
        activity.setReason(reason != null && !reason.isBlank() ? reason : "Không có lý do cụ thể");
        // [FIX LỖI 395] set HandledBy bằng ID
        Users admin = getCurrentAdmin();
        activity.setHandledBy(admin != null ? admin.getId() : null);
        activity.setHandledAt(LocalDateTime.now());

        Activities savedActivity = activityRepository.save(activity);

        sendNotificationSafe(savedActivity, "Hoạt động bị từ chối", "Lý do: " + activity.getReason(), 3);
    }

    // --- CANCEL ---
    @Override
    @Transactional
    public void cancelActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);

        if (activity.getStatus() != 0 && activity.getStatus() != 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ có thể hủy hoạt động đang Chờ duyệt hoặc đã được Duyệt.");
        }

        activity.setStatus(4);
        activity.setReason(reason != null && !reason.isBlank() ? reason : "Sự cố ngoài ý muốn");
        // [FIX LỖI 415] set HandledBy bằng ID
        Users admin = getCurrentAdmin();
        activity.setHandledBy(admin != null ? admin.getId() : null);
        activity.setHandledAt(LocalDateTime.now());

        Activities savedActivity = activityRepository.save(activity);

        sendNotificationSafe(savedActivity, "Hoạt động đã bị hủy",
                "Hoạt động '" + savedActivity.getTitle() + "' đã bị hủy. Lý do: " + activity.getReason(), 3);
    }

    private void sendNotificationSafe(Activities activity, String title, String message, Integer type) {
        try {
            NotificationRequest notiRequest = activityMapper.toNotificationRequest(activity, title, message, type);
            notificationClient.createNotification(notiRequest);
        } catch (Exception e) {
            log.error("Failed to send notification for activity {}: {}", activity.getId(), e.getMessage());
        }
    }

    private Organizers getOrCreateOrganizer(Users user) {
        return organizerRepository.findById(user.getId())
                .orElseGet(() -> {
                    log.info("Hệ thống tự động thêm User {} vào bảng Organizers...", user.getUsername());

                    ProfileDto profile = getProfileData(user);
                    String displayName = (profile != null && profile.getFullName() != null)
                            ? profile.getFullName()
                            : user.getUsername();

                    // [FIX LỖI 444] Đổi từ .user(user) thành .id(user.getId())
                    Organizers newOrg = Organizers.builder()
                            .id(user.getId())
                            .name(displayName)
                            .build();

                    // [FIX LỖI 451] Đổi thành setDepartmentId()
                    if (profile != null && profile.getDepartmentId() != null) {
                        newOrg.setDepartmentId(profile.getDepartmentId());
                    }

                    return organizerRepository.save(newOrg);
                });
    }

    public void deleteOldImage(String oldImg) {
        cloudinaryService.deleteImageByUrl(oldImg);
    }

    public String getQrCodeForActivity(Long activityId) {
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED ,"Không tìm thấy hoạt động có ID: " + activityId));

        String qrToken = activity.getQrCodeToken();

        if (qrToken == null || qrToken.isEmpty()) {
            throw new AppException(ErrorCode.INCORRECT_VALUE, "Hoạt động này chưa được cấp mã QR Token!");
        }

        return qrCodeService.generateQRCodeBase64(qrToken, 300, 300);
    }

    @Override
    public ActivityStatsResponse getActivityStats() {
        long pending = activityRepository.countByStatus(0);
        long approved = activityRepository.countByStatus(1);
        long rejected = activityRepository.countByStatus(2);

        return ActivityStatsResponse.builder()
                .pendingReview(pending)
                .approvedThisTerm(approved)
                .rejected(rejected)
                .build();
    }

    // ============ HELPER ==================
    private Activities getActivityForAction(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));

        int currentStatus = activity.getStatus();
        if (currentStatus == 2 || currentStatus == 4) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể thao tác trên hoạt động đã bị Từ chối hoặc đã Hủy!");
        }
        return activity;
    }

    private Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        return null;
    }

}