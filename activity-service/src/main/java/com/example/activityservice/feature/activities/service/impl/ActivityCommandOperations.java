package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityRequest;
import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.dto.ActivityScheduleQrCodeResponse;
import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.activities.service.ActivityRegistrationNotificationService;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.activitySchedule.mapper.ActivityScheduleMapper;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.benefits.mapper.BenefitMapper;
import com.example.activityservice.feature.benefits.model.Benefits;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.benefits.service.BenefitValidationService;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.locations.model.Location;
import com.example.activityservice.feature.locations.repository.LocationRepository;
import com.example.activityservice.feature.locations.dto.LocationBookingResponse;
import com.example.activityservice.feature.locations.service.ActivityLocationBookingService;
import com.example.activityservice.feature.organizers.model.Organizers;
import com.example.activityservice.feature.organizers.repository.OrganizerRepository;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.feature.users.service.LocalUserResolver;
import com.example.activityservice.feature.users.service.StudentRepresentativePermissionClient;
import com.example.activityservice.feature.users.dto.RepresentativeActivityPermissionResponse;
import com.example.activityservice.service.CloudinaryService;
import com.example.activityservice.service.QRCodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.event.ActivityDeletedEvent;
import com.example.event.kafka.KafkaTopics;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityCommandOperations {

    private static final int ROLE_STUDENT = 1;
    private static final int ROLE_DEPARTMENT = 2;
    private static final int ROLE_ADMIN = 3;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_DRAFT = 3;

    private final ActivityRepository activityRepository;
    private final SemesterRepository semesterRepository;
    private final OrganizerRepository organizerRepository;
    private final LocalUserResolver localUserResolver;
    private final BenefitRepository benefitRepository;
    private final ActivityMapper activityMapper;
    private final ActivityScheduleMapper scheduleMapper;
    private final BenefitMapper benefitMapper;
    private final BenefitValidationService benefitValidationService;
    private final CloudinaryService cloudinaryService;
    private final QRCodeService qrCodeService;
    private final ActivityScheduleRepository scheduleRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ActivityEventProducer activityEventProducer;
    private final NotificationCommandProducer notificationCommandProducer;
    private final ActivityResponseAssembler responseAssembler;
    private final ActivityCacheService activityCacheService;
    private final StudentRepresentativePermissionClient representativePermissionClient;
    private final ActivityLocationBookingService locationBookingService;
    private final LocationRepository locationRepository;
    private final ActivityRegistrationNotificationService registrationNotificationService;
    private final UserRepository userRepository;
    private final ActivityAccessSupport accessSupport;

    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        boolean isDraft = Integer.valueOf(STATUS_DRAFT).equals(request.getStatus());
        Users currentUser = resolveCurrentCreator();
        boolean studentCreator = isStudent(currentUser);
        RepresentativeActivityPermissionResponse representativePermission = studentCreator
                ? validateStudentRepresentativePermission(currentUser)
                : null;
        if (studentCreator && isDraft) {
            validateStudentDraftMinimum(request);
        }
        Semesters semester = resolveSemesterForCreate(request, isDraft);
        Organizers organizer = resolveOrganizer(request, currentUser, studentCreator);

        Activities activity = activityMapper.toEntity(request, organizer);
        if (semester != null) {
            activity.setSemester(semester);
        }
        applyCurrentCreator(activity, currentUser);
        if (studentCreator) {
            applyStudentRepresentativeFlow(activity, representativePermission, isDraft);
        } else {
            applyInstitutionFlow(activity, currentUser, isDraft);
        }
        applySchedules(activity, request, currentUser);

        Activities savedActivity = activityRepository.save(activity);
        List<BenefitResponse> savedBenefits = replaceActivityBenefits(savedActivity, request.getBenefits());
        List<LocationBookingResponse> savedBookings = locationBookingService.replaceBookings(
                savedActivity,
                request.getLocationBookings(),
                currentUser,
                savedActivity.getStatus());

        if (savedActivity.getStatus() == STATUS_APPROVED) {
            log.info("Hoạt động đã được tạo và phê duyệt tự động!");
        } else if (savedActivity.getStatus() == STATUS_DRAFT) {
            log.info("Bản nháp hoạt động đã được lưu thành công!");
        } else {
            log.info("Hoạt động đã được tạo và gửi duyệt thành công!");
        }

        ActivityResponse response = responseAssembler.toResponse(savedActivity);
        response.setBenefits(savedBenefits);
        response.setLocationBookings(savedBookings);
        activityEventProducer.publishCreated(savedActivity);
        if (savedActivity.getStatus() == STATUS_APPROVED) {
            if (!shouldSuppressApprovalNotification(savedActivity)) {
                activityEventProducer.publishApproved(savedActivity);
            }
            registrationNotificationService.notifyIfRegistrationOpen(savedActivity);
        } else if (savedActivity.getStatus() != STATUS_DRAFT) {
            activityEventProducer.publishSubmitted(savedActivity, resolveSubmissionReviewerIds(savedActivity));
        }
        activityCacheService.evictActivityListCaches();
        return response;
    }

    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activities existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));
        Users currentUser = resolveCurrentCreator();
        accessSupport.ensureCurrentUserCanManageActivity(existingActivity);
        RepresentativeActivityPermissionResponse representativePermission = null;

        if (isStudent(currentUser)) {
            representativePermission = validateStudentRepresentativePermission(currentUser);
            if (Integer.valueOf(STATUS_DRAFT).equals(request.getStatus())) {
                validateStudentDraftMinimum(request);
            }
        }

        boolean canEditApprovedDepartmentActivity = canEditApprovedDepartmentActivity(existingActivity, currentUser);
        boolean editablePendingOrDraft = existingActivity.getStatus() == STATUS_PENDING
                || existingActivity.getStatus() == STATUS_DRAFT;
        if (!editablePendingOrDraft && !canEditApprovedDepartmentActivity) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chỉ có thể chỉnh sửa hoạt động đang chờ duyệt, bản nháp hoặc hoạt động trực tiếp của Đơn vị khi chưa tới thời gian tổ chức.");
        }
        if (isDepartment(currentUser) && isPendingAdminApproval(existingActivity)) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoạt động đang chờ admin duyệt không thể chỉnh sửa. Vui lòng liên hệ admin để hỗ trợ.");
        }
        if (canEditApprovedDepartmentActivity
                && (request.getStartDate() == null || !request.getStartDate().isAfter(LocalDateTime.now()))) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thời gian tổ chức mới phải sau thời gian hiện tại.");
        }

        Integer previousStatus = existingActivity.getStatus();
        boolean keepDraft = Integer.valueOf(STATUS_DRAFT).equals(request.getStatus())
                || (request.getStatus() == null && Integer.valueOf(STATUS_DRAFT).equals(existingActivity.getStatus()));
        String oldCoverImg = existingActivity.getCoverImage();
        String oldThumbnailImg = existingActivity.getThumbnail();

        activityMapper.updateEntityFromRequest(request, existingActivity);
        if (isStudent(currentUser)) {
            applyStudentRepresentativeFlow(existingActivity, representativePermission, keepDraft);
        } else if (isStudentRepresentativeActivity(existingActivity)) {
            existingActivity.setStatus(keepDraft ? STATUS_DRAFT : STATUS_PENDING);
        } else {
            applyInstitutionFlow(existingActivity, currentUser, keepDraft);
        }

        if (request.getCoverImage() != null && !request.getCoverImage().equals(oldCoverImg)) {
            deleteOldImage(oldCoverImg);
        }
        if (request.getThumbnail() != null && !request.getThumbnail().equals(oldThumbnailImg)) {
            deleteOldImage(oldThumbnailImg);
        }

        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            Semesters newSemester = semesterRepository.findSemesterByDate(activityDate)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày tổ chức không thuộc học kỳ nào!"));
            existingActivity.setSemester(newSemester);
        }

        if (request.getOrganizerId() != null &&
                (existingActivity.getOrganizer() == null
                        || !Objects.equals(existingActivity.getOrganizer().getUserId(), request.getOrganizerId()))) {
            Users user = localUserResolver.resolveById(request.getOrganizerId());
            validateOrganizerDepartmentScope(user, currentUser);
            Organizers newOrganizer = getOrCreateOrganizer(user);
            existingActivity.setOrganizer(newOrganizer);
        }

        if (request.getSchedules() != null) {
            if (existingActivity.getSchedules() != null) {
                existingActivity.getSchedules().clear();
            } else {
                existingActivity.setSchedules(new ArrayList<>());
            }
            List<ActivitySchedule> newSchedules = buildSchedules(existingActivity, request, currentUser);
            existingActivity.getSchedules().addAll(newSchedules);
        }

        Activities updatedActivity = activityRepository.save(existingActivity);
        if (request.getBenefits() != null) {
            replaceActivityBenefits(updatedActivity, request.getBenefits());
        }
        if (request.getLocationBookings() != null) {
            locationBookingService.replaceBookings(
                    updatedActivity,
                    request.getLocationBookings(),
                    existingActivity.getCreatedBy(),
                    updatedActivity.getStatus());
        }

        activityEventProducer.publishUpdated(updatedActivity);
        publishStatusTransitionEvent(previousStatus, updatedActivity);
        registrationNotificationService.notifyIfRegistrationOpen(updatedActivity);

        ActivityResponse response = responseAssembler.toResponse(updatedActivity);
        response.setBenefits(responseAssembler.getActivityBenefits(updatedActivity.getId()));
        response.setLocationBookings(locationBookingService.getBookingsByActivityId(updatedActivity.getId()));
        activityCacheService.evictActivityListCaches();
        return response;
    }

    @Transactional
    public void deleteActivity(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));
        Users currentUser = resolveCurrentCreator();
        accessSupport.ensureCurrentUserCanManageActivity(activity);

        if (isDepartment(currentUser) && isPendingAdminApproval(activity)) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoạt động đang chờ admin duyệt không thể xóa. Hãy gửi yêu cầu hỗ trợ lên admin.");
        }

        Integer currentStatus = activity.getStatus();
        if (currentStatus != null && (currentStatus == 1 || currentStatus == 4)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể xóa hoạt động đã duyệt hoặc hủy.");
        }

        deleteOldImage(activity.getCoverImage());
        deleteOldImage(activity.getThumbnail());

        activityRepository.deleteById(id);
        kafkaTemplate.send("iact.activity.deleted", new ActivityDeletedEvent(id));
        activityEventProducer.publishDeleted(id);
        activityCacheService.evictActivityListCaches();
        log.info("Đã gửi sự kiện Kafka yêu cầu xóa thông báo cho hoạt động ID: {}", id);
    }

    @Transactional
    public String getQrCodeForActivity(Long activityId) {
        Activities activity = resolveManageableActivity(activityId);
        List<ActivitySchedule> schedules = scheduleRepository.findByActivityId(activityId);
        if (schedules.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoạt động chưa có buổi chi tiết để tạo mã QR điểm danh.");
        }
        if (schedules.size() > 1) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoạt động có nhiều buổi. Vui lòng chọn buổi cụ thể để lấy mã QR.");
        }
        return buildScheduleQrResponse(activity, schedules.get(0)).getQrCodeImage();
    }

    @Transactional
    public ActivityScheduleQrCodeResponse getQrCodeForSchedule(Long activityId, Long scheduleId) {
        Activities activity = resolveManageableActivity(activityId);
        ActivitySchedule schedule = scheduleRepository.findByIdAndActivityId(scheduleId, activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Không tìm thấy buổi của hoạt động này."));
        return buildScheduleQrResponse(activity, schedule);
    }

    @Transactional
    public List<ActivityScheduleQrCodeResponse> getQrCodesForActivity(Long activityId) {
        Activities activity = resolveManageableActivity(activityId);
        List<ActivitySchedule> schedules = scheduleRepository.findByActivityId(activityId);
        if (schedules.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoạt động chưa có buổi chi tiết để tạo mã QR điểm danh.");
        }
        schedules.sort(Comparator.comparing(
                (ActivitySchedule schedule) -> schedule.getStartTime(),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return schedules.stream()
                .map(schedule -> buildScheduleQrResponse(activity, schedule))
                .toList();
    }

    private Activities resolveManageableActivity(Long activityId) {
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));
        accessSupport.ensureCurrentUserCanManageActivity(activity);
        return activity;
    }

    private ActivityScheduleQrCodeResponse buildScheduleQrResponse(Activities activity, ActivitySchedule schedule) {
        schedule = ensureScheduleQrToken(schedule);
        String payload = buildScheduleQrPayload(activity, schedule);
        return ActivityScheduleQrCodeResponse.builder()
                .activityId(activity.getId())
                .activityTitle(activity.getTitle())
                .scheduleId(schedule.getId())
                .scheduleTitle(schedule.getTitle())
                .scheduleStartTime(schedule.getStartTime())
                .scheduleEndTime(schedule.getEndTime())
                .location(schedule.getLocation())
                .qrCodeImage(qrCodeService.generateQRCodeBase64(payload, 300, 300))
                .build();
    }

    private ActivitySchedule ensureScheduleQrToken(ActivitySchedule schedule) {
        if (schedule.getQrCodeToken() == null || schedule.getQrCodeToken().isBlank()) {
            schedule.setQrCodeToken(java.util.UUID.randomUUID().toString());
            return scheduleRepository.save(schedule);
        }
        return schedule;
    }

    private String buildScheduleQrPayload(Activities activity, ActivitySchedule schedule) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ACTIVITY_SCHEDULE_ATTENDANCE");
        payload.put("version", 1);
        payload.put("activityId", activity.getId());
        payload.put("scheduleId", schedule.getId());
        payload.put("verifyCode", schedule.getQrCodeToken());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể tạo dữ liệu mã QR.");
        }
    }

    public void deleteOldImage(String oldImg) {
        cloudinaryService.deleteImageByUrl(oldImg);
    }

    @Transactional
    public void requestAdminSupport(Long id, String reason) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));
        Users currentUser = resolveCurrentCreator();
        if (!isDepartment(currentUser) || currentUser.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ Đơn vị mới được gửi yêu cầu hỗ trợ lên admin.");
        }
        if (!Objects.equals(currentUser.getDepartmentId(), activity.getDepartmentId())
                || !isPendingAdminApproval(activity)) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chỉ có hoạt động đang chờ admin duyệt của Đơn vị mới được gửi yêu cầu hỗ trợ.");
        }

        List<Long> adminIds = userRepository.findActiveAdminUserIds();
        if (adminIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không tìm thấy tài khoản admin đang hoạt động.");
        }

        String supportReason = isBlank(reason)
                ? "Đề nghị admin hỗ trợ xử lý hoạt động đang chờ duyệt."
                : reason.trim();
        String title = "Yêu cầu hỗ trợ hủy hoạt động";
        String message = "Đơn vị của bạn cần admin hỗ trợ hủy hoạt động '" + activity.getTitle()
                + "'. Lý do: " + supportReason;

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("userIds", adminIds);
        payload.put("activityId", activity.getId());
        payload.put("title", title);
        payload.put("message", message);
        payload.put("content", message);
        payload.put("type", 2);
        payload.put("referenceType", "activity-admin-support");
        payload.put("sourceTopic", KafkaTopics.NOTIFICATION_BROADCAST_REQUESTED);
        payload.put("sourceEventId", "activity-admin-support:" + activity.getId());

        notificationCommandProducer.publishBroadcastRequested("activity-admin-support:" + activity.getId(), payload);
    }

    private Semesters resolveSemesterForCreate(ActivityRequest request, boolean isDraft) {
        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            Optional<Semesters> matchedSemester = semesterRepository.findSemesterByDate(activityDate);
            return isDraft
                    ? matchedSemester.orElse(null)
                    : matchedSemester.orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày bắt đầu tổ chức (" + activityDate
                                    + ") không thuộc bất kỳ học kỳ nào đang được cấu hình!"));
        }
        if (!isDraft) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn ngày bắt đầu tổ chức!");
        }
        return null;
    }

    private Organizers resolveOrganizer(ActivityRequest request, Users currentUser, boolean studentCreator) {
        if (studentCreator) {
            return getOrCreateOrganizer(currentUser);
        }
        if (request.getOrganizerId() == null) {
            return null;
        }
        Users user = localUserResolver.resolveById(request.getOrganizerId());
        validateOrganizerDepartmentScope(user, currentUser);
        return getOrCreateOrganizer(user);
    }

    private Users resolveCurrentCreator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return localUserResolver.resolveByUsername(authentication.getName());
        }
        return null;
    }

    private void applyCurrentCreator(Activities activity, Users currentUser) {
        if (currentUser == null) {
            return;
        }
        activity.setCreatedBy(currentUser);
        activity.setCreatedByUsername(currentUser.getUsername());
        activity.setDepartmentId(currentUser.getDepartmentId());
    }

    private boolean isStudent(Users user) {
        return user != null && Integer.valueOf(ROLE_STUDENT).equals(user.getRoleType());
    }

    private RepresentativeActivityPermissionResponse validateStudentRepresentativePermission(Users currentUser) {
        RepresentativeActivityPermissionResponse permission = representativePermissionClient
                .getCurrentStudentActivityPermission();
        if (permission == null
                || !permission.isCanCreateActivity()
                || !Objects.equals(permission.getStudentId(), currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Chỉ sinh viên đại diện lớp/chi đoàn mới được đăng ký tổ chức hoạt động.");
        }
        return permission;
    }

    private void validateStudentDraftMinimum(ActivityRequest request) {
        if (isBlank(request.getTitle())
                || isBlank(request.getDescription())
                || isBlank(request.getContent())
                || request.getRegistrationStart() == null
                || request.getRegistrationEnd() == null
                || request.getStartDate() == null
                || request.getEndDate() == null
                || request.getMaxParticipants() == null
                || request.getMaxParticipants() <= 0) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Bản nháp cần có tên, mô tả, nội dung, thời gian đăng ký, thời gian tổ chức và sức chứa.");
        }

        if (!request.getRegistrationEnd().isAfter(request.getRegistrationStart())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thời gian đóng đăng ký phải sau thời gian mở đăng ký.");
        }
        if (!request.getStartDate().isAfter(request.getRegistrationEnd())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thời gian tổ chức phải sau khi đóng đăng ký.");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void applyStudentRepresentativeFlow(
            Activities activity,
            RepresentativeActivityPermissionResponse permission,
            boolean isDraft) {
        activity.setDepartmentId(
                permission.getDepartmentId() != null ? permission.getDepartmentId() : activity.getDepartmentId());
        activity.setIsExternal(false);
        activity.setIsFaculty(true);
        activity.setStatus(isDraft ? STATUS_DRAFT : STATUS_PENDING);
        activity.setRequiresAdminApproval(false);
    }

    private void applyInstitutionFlow(Activities activity, Users currentUser, boolean isDraft) {
        if (isDraft || currentUser == null) {
            return;
        }
        if (Integer.valueOf(ROLE_ADMIN).equals(currentUser.getRoleType())) {
            activity.setRequiresAdminApproval(false);
            activity.setStatus(STATUS_APPROVED);
            activity.setHandledBy(currentUser);
            activity.setHandledAt(LocalDateTime.now());
            return;
        }
        if (Integer.valueOf(ROLE_DEPARTMENT).equals(currentUser.getRoleType())) {
            boolean requiresAdminApproval = Boolean.TRUE.equals(activity.getRequiresAdminApproval());
            activity.setRequiresAdminApproval(requiresAdminApproval);
            if (requiresAdminApproval) {
                activity.setStatus(STATUS_PENDING);
                activity.setHandledBy(null);
                activity.setHandledAt(null);
                return;
            }
            activity.setStatus(STATUS_APPROVED);
            activity.setHandledBy(currentUser);
            activity.setHandledAt(LocalDateTime.now());
        }
    }

    private boolean isDepartment(Users user) {
        return user != null && Integer.valueOf(ROLE_DEPARTMENT).equals(user.getRoleType());
    }

    private boolean canEditApprovedDepartmentActivity(Activities activity, Users currentUser) {
        return isDepartment(currentUser)
                && activity != null
                && Integer.valueOf(STATUS_APPROVED).equals(activity.getStatus())
                && isDepartmentCreatedActivity(activity)
                && !Boolean.TRUE.equals(activity.getRequiresAdminApproval())
                && activity.getStartDate() != null
                && activity.getStartDate().isAfter(LocalDateTime.now());
    }

    private boolean isDepartmentCreatedActivity(Activities activity) {
        return activity != null
                && activity.getCreatedBy() != null
                && Integer.valueOf(ROLE_DEPARTMENT).equals(activity.getCreatedBy().getRoleType());
    }

    private boolean isStudentRepresentativeActivity(Activities activity) {
        return activity.getCreatedBy() != null && isStudent(activity.getCreatedBy());
    }

    private boolean isDepartmentApprovalRequiredActivity(Activities activity) {
        return activity != null
                && activity.getCreatedBy() != null
                && Integer.valueOf(ROLE_DEPARTMENT).equals(activity.getCreatedBy().getRoleType())
                && Boolean.TRUE.equals(activity.getRequiresAdminApproval());
    }

    private boolean isPendingAdminApproval(Activities activity) {
        return activity != null
                && Integer.valueOf(STATUS_PENDING).equals(activity.getStatus())
                && isDepartmentApprovalRequiredActivity(activity);
    }

    private boolean shouldSuppressApprovalNotification(Activities activity) {
        return activity != null
                && activity.getCreatedBy() != null
                && Integer.valueOf(ROLE_DEPARTMENT).equals(activity.getCreatedBy().getRoleType())
                && !Boolean.TRUE.equals(activity.getRequiresAdminApproval());
    }

    private void publishStatusTransitionEvent(Integer previousStatus, Activities activity) {
        Integer currentStatus = activity.getStatus();
        if (Objects.equals(previousStatus, currentStatus)) {
            return;
        }
        if (Integer.valueOf(STATUS_APPROVED).equals(currentStatus)) {
            if (!shouldSuppressApprovalNotification(activity)) {
                activityEventProducer.publishApproved(activity);
            }
        } else if (Integer.valueOf(STATUS_PENDING).equals(currentStatus)) {
            activityEventProducer.publishSubmitted(activity, resolveSubmissionReviewerIds(activity));
        }
    }

    private List<Long> resolveSubmissionReviewerIds(Activities activity) {
        if (activity == null || activity.getDepartmentId() == null) {
            return List.of();
        }
        if (isDepartmentApprovalRequiredActivity(activity)) {
            return userRepository.findActiveAdminUserIds();
        }
        return userRepository.findActiveDepartmentUserIdsByDepartmentId(activity.getDepartmentId());
    }

    private void applySchedules(Activities activity, ActivityRequest request, Users currentUser) {
        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            List<ActivitySchedule> schedulesList = buildSchedules(activity, request, currentUser);
            activity.setSchedules(schedulesList);
        }
    }

    private List<ActivitySchedule> buildSchedules(Activities activity, ActivityRequest request, Users currentUser) {
        List<ActivitySchedule> schedulesList = scheduleMapper.toEntityList(request.getSchedules());
        for (int index = 0; index < schedulesList.size(); index++) {
            ActivitySchedule schedule = schedulesList.get(index);
            schedule.setActivity(activity);
            Long locationId = request.getSchedules().get(index).getLocationId();
            if (locationId == null) {
                continue;
            }
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Không tìm thấy địa điểm của buổi hoạt động."));
            schedule.setLocationRef(location);
            schedule.setLocation(location.getName());
        }
        return schedulesList;
    }

    private List<BenefitResponse> replaceActivityBenefits(
            Activities activity,
            List<BenefitResponse> requestedBenefits) {
        if (activity.getId() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể lưu quyền lợi khi hoạt động chưa tồn tại!");
        }

        benefitRepository.deleteByActivityId(activity.getId());
        if (requestedBenefits == null || requestedBenefits.isEmpty()) {
            return List.of();
        }

        List<Benefits> benefits = requestedBenefits.stream()
                .map(request -> {
                    Categories category = benefitValidationService.validateAndGetCategory(
                            request.getCategoryId(), request.getPoint(), request.getType());
                    return Benefits.builder()
                            .activity(activity)
                            .category(category)
                            .type(request.getType())
                            .point(request.getPoint())
                            .build();
                })
                .toList();

        return benefitRepository.saveAll(benefits).stream()
                .map(benefitMapper::toResponse)
                .toList();
    }

    private Organizers getOrCreateOrganizer(Users user) {
        return organizerRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String displayName = (user.getFullName() != null) ? user.getFullName() : user.getUsername();
                    Organizers newOrg = Organizers.builder()
                            .userId(user.getId())
                            .fullName(displayName)
                            .departmentId(user.getDepartmentId())
                            .build();
                    return organizerRepository.save(newOrg);
                });
    }

    private void validateOrganizerDepartmentScope(Users organizerUser, Users currentUser) {
        if (currentUser == null || !Integer.valueOf(ROLE_DEPARTMENT).equals(currentUser.getRoleType())) {
            return;
        }
        if (currentUser.getDepartmentId() == null
                || organizerUser == null
                || organizerUser.getDepartmentId() == null
                || !Objects.equals(currentUser.getDepartmentId(), organizerUser.getDepartmentId())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Đơn vị chỉ được chọn người tổ chức thuộc đúng đơn vị của mình.");
        }
    }
}
