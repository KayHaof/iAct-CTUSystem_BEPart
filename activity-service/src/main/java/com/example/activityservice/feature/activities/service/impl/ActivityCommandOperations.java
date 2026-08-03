package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityRequest;
import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.activities.service.ActivityRegistrationNotificationService;
import com.example.activityservice.feature.notification.kafka.NotificationCommandProducer;
import com.example.activityservice.feature.activitySchedule.mapper.ActivityScheduleMapper;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
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
    private static final int STATUS_REJECTED = 2;
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
            log.info("Hoat dong da duoc tao va phe duyet tu dong!");
        } else if (savedActivity.getStatus() == STATUS_DRAFT) {
            log.info("Ban nhap hoat dong da duoc luu thanh cong!");
        } else {
            log.info("Hoat dong da duoc tao va gui duyet thanh cong!");
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
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay hoat dong!"));
        Users currentUser = resolveCurrentCreator();
        accessSupport.ensureCurrentUserCanManageActivity(existingActivity);
        RepresentativeActivityPermissionResponse representativePermission = null;

        if (isStudent(currentUser)) {
            representativePermission = validateStudentRepresentativePermission(currentUser);
            if (Integer.valueOf(STATUS_DRAFT).equals(request.getStatus())) {
                validateStudentDraftMinimum(request);
            }
        }

        if (existingActivity.getStatus() != STATUS_PENDING && existingActivity.getStatus() != STATUS_DRAFT) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chi co the chinh sua hoat dong dang cho duyet hoac ban nhap.");
        }
        if (isDepartment(currentUser) && isPendingAdminApproval(existingActivity)) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoat dong dang cho admin duyet khong the chinh sua. Vui long lien he admin de ho tro.");
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
                            "Ngay to chuc khong thuoc hoc ky nao!"));
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
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !"));
        Users currentUser = resolveCurrentCreator();
        accessSupport.ensureCurrentUserCanManageActivity(activity);

        if (isDepartment(currentUser) && isPendingAdminApproval(activity)) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Hoat dong dang cho admin duyet khong the xoa. Hay gui yeu cau ho tro len admin.");
        }

        Integer currentStatus = activity.getStatus();
        if (currentStatus != null && (currentStatus == 1 || currentStatus == 4)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khong the xoa hoat dong da duyet hoac huy.");
        }

        deleteOldImage(activity.getCoverImage());
        deleteOldImage(activity.getThumbnail());

        activityRepository.deleteById(id);
        kafkaTemplate.send("iact.activity.deleted", new ActivityDeletedEvent(id));
        activityEventProducer.publishDeleted(id);
        activityCacheService.evictActivityListCaches();
        log.info("Da gui event Kafka yeu cau xoa thong bao cho Activity ID: {}", id);
    }

    public String getQrCodeForActivity(Long activityId) {
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay hoat dong!"));
        accessSupport.ensureCurrentUserCanManageActivity(activity);
        if (activity.getQrCodeToken() == null || activity.getQrCodeToken().isBlank()) {
            activity.setQrCodeToken(java.util.UUID.randomUUID().toString());
            activity = activityRepository.save(activity);
        }
        return qrCodeService.generateQRCodeBase64(activity.getQrCodeToken(), 300, 300);
    }

    public void deleteOldImage(String oldImg) {
        cloudinaryService.deleteImageByUrl(oldImg);
    }

    @Transactional
    public void requestAdminSupport(Long id, String reason) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay hoat dong!"));
        Users currentUser = resolveCurrentCreator();
        if (!isDepartment(currentUser) || currentUser.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chi don vi moi duoc gui yeu cau ho tro len admin.");
        }
        if (!Objects.equals(currentUser.getDepartmentId(), activity.getDepartmentId())
                || !isPendingAdminApproval(activity)) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chi co hoat dong dang cho admin duyet cua don vi moi duoc gui ho tro.");
        }

        List<Long> adminIds = userRepository.findActiveAdminUserIds();
        if (adminIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khong tim thay tai khoan admin hoat dong.");
        }

        String supportReason = isBlank(reason)
                ? "De nghi admin ho tro xu ly hoat dong dang cho duyet."
                : reason.trim();
        String title = "Yeu cau ho tro huy hoat dong";
        String message = "Don vi cua ban can admin ho tro huy hoat dong '" + activity.getTitle()
                + "'. Ly do: " + supportReason;

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
                            "Ngay bat dau to chuc (" + activityDate
                                    + ") khong thuoc bat ky hoc ky nao dang duoc cau hinh!"));
        }
        if (!isDraft) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui long chon ngay bat dau to chuc!");
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
        RepresentativeActivityPermissionResponse permission =
                representativePermissionClient.getCurrentStudentActivityPermission();
        if (permission == null
                || !permission.isCanCreateActivity()
                || !Objects.equals(permission.getStudentId(), currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Chi sinh vien dai dien lop/chi doan moi duoc dang ky to chuc hoat dong.");
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
                    "Ban nhap can co ten, mo ta, noi dung, thoi gian dang ky, thoi gian to chuc va suc chua.");
        }

        if (!request.getRegistrationEnd().isAfter(request.getRegistrationStart())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thoi gian dong dang ky phai sau thoi gian mo dang ky.");
        }
        if (!request.getStartDate().isAfter(request.getRegistrationEnd())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thoi gian to chuc phai sau khi dong dang ky.");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Thoi gian ket thuc phai sau thoi gian bat dau.");
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

    private boolean isFacultyInternalActivity(Activities activity) {
        return Boolean.TRUE.equals(activity.getIsFaculty()) && !Boolean.TRUE.equals(activity.getIsExternal());
    }

    private boolean isDepartment(Users user) {
        return user != null && Integer.valueOf(ROLE_DEPARTMENT).equals(user.getRoleType());
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
                            "Khong tim thay dia diem cua buoi hoat dong."));
            schedule.setLocationRef(location);
            schedule.setLocation(location.getName());
        }
        return schedulesList;
    }

    private List<BenefitResponse> replaceActivityBenefits(
            Activities activity,
            List<BenefitResponse> requestedBenefits) {
        if (activity.getId() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khong the luu quyen loi khi hoat dong chua ton tai!");
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
                    "Don vi chi duoc chon nguoi to chuc thuoc dung don vi cua minh.");
        }
    }
}
