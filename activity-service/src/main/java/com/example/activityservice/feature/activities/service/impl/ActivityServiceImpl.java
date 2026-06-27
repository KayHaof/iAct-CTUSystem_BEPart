package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.*;
import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.feature.users.service.LocalUserResolver;
import com.example.dto.PageDTO;
import com.example.event.ActivityDeletedEvent;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityService;
import com.example.activityservice.feature.activities.specification.ActivitySpecification;
import com.example.activityservice.feature.activitySchedule.mapper.ActivityScheduleMapper;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.benefits.mapper.BenefitMapper;
import com.example.activityservice.feature.benefits.model.Benefits;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.benefits.service.BenefitValidationService;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.organizers.model.Organizers;
import com.example.activityservice.feature.organizers.repository.OrganizerRepository;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;

import com.example.activityservice.service.CloudinaryService;
import com.example.activityservice.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final SemesterRepository semesterRepository;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final LocalUserResolver localUserResolver;
    private final RegistrationRepository registrationRepository;
    private final BenefitRepository benefitRepository;

    private final ActivityMapper activityMapper;
    private final ActivityScheduleMapper scheduleMapper;
    private final BenefitMapper benefitMapper;
    private final BenefitValidationService benefitValidationService;

    private final CloudinaryService cloudinaryService;
    private final QRCodeService qrCodeService;

    // ĐÃ THÊM KAFKA TẠI ĐÂY
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ActivityEventProducer activityEventProducer;

    // --- CREATE ---
    @Override
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        boolean isDraft = Integer.valueOf(3).equals(request.getStatus());
        Semesters semester = null;
        if (request.getStartDate() != null) {
            LocalDate activityDate = request.getStartDate().toLocalDate();
            Optional<Semesters> matchedSemester = semesterRepository.findSemesterByDate(activityDate);
            semester = isDraft
                    ? matchedSemester.orElse(null)
                    : matchedSemester.orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                            "Ngày bắt đầu tổ chức (" + activityDate
                                    + ") không thuộc bất kỳ Học kỳ nào đang được cấu hình!"));
        } else if (!isDraft) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn Ngày bắt đầu tổ chức!");
        }

        Organizers organizer = null;
        if (request.getOrganizerId() != null) {
            Long userId = request.getOrganizerId();
            Users user = localUserResolver.resolveById(userId);
            organizer = getOrCreateOrganizer(user);
        }

        Activities activity = activityMapper.toEntity(request, organizer);
        if (semester != null) {
            activity.setSemester(semester);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String currentUsername = authentication.getName();
            Users currentUser = localUserResolver.resolveByUsername(currentUsername);

            activity.setCreatedBy(currentUser);
            activity.setCreatedByUsername(currentUsername);

            // ĐÃ FIX: Lấy departmentId trực tiếp từ local Users
            activity.setDepartmentId(currentUser.getDepartmentId());
        }

        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            List<ActivitySchedule> schedulesList = scheduleMapper.toEntityList(request.getSchedules());
            schedulesList.forEach(schedule -> schedule.setActivity(activity));
            activity.setSchedules(schedulesList);
        }

        Activities savedActivity = activityRepository.save(activity);
        List<BenefitResponse> savedBenefits = replaceActivityBenefits(savedActivity, request.getBenefits());

        if (savedActivity.getStatus() != 3) {
            log.info("Hoạt động đã được tạo và gửi duyệt thành công!");
        } else {
            log.info("Bản nháp hoạt động đã được lưu thành công!");
        }

        ActivityResponse response = activityMapper.toResponse(savedActivity);
        response.setBenefits(savedBenefits);
        activityEventProducer.publishCreated(savedActivity);
        if (savedActivity.getStatus() != 3) {
            activityEventProducer.publishSubmitted(savedActivity);
        }
        return response;
    }

    // --- READ DETAILS ---
    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động"));

        ActivityResponse response = activityMapper.toResponse(activity);
        response.setBenefits(getActivityBenefits(activity.getId()));
        long count = registrationRepository.countByActivityIdAndStatusNot(id, 2);
        response.setRegisteredCount((int) count);
        return response;
    }

    @Override
    public ActivityTimeLocationResponse getActivityTimesAndLocation(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Không tìm thấy hoạt động với ID: " + id));
        return activityMapper.toTimeResponse(activity);
    }

    // --- READ ALL ---
    @Override
    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getAllActivities(String keyword, String level, String status, Long departmentId,
            Pageable pageable) {
        Long userDeptId = null;
        boolean isAdmin = false;
        boolean isDepartment = false;
        boolean isStudent = true;
        Users currentUser = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
            isDepartment = authentication.getAuthorities().stream()
                    .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_DEPARTMENT"));
            isStudent = !isAdmin && !isDepartment;

            String username = authentication.getName();
            Optional<Users> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                if (isAdmin || isDepartment) {
                    Optional<Organizers> orgOpt = organizerRepository.findById(currentUser.getId());
                    if (orgOpt.isPresent() && orgOpt.get().getDepartmentId() != null) {
                        userDeptId = orgOpt.get().getDepartmentId();
                    }
                } else {
                    // ĐÃ FIX: Lấy departmentId trực tiếp từ local Users
                    userDeptId = currentUser.getDepartmentId();
                }
            }
        }

        Specification<Activities> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (isStudent) {
            spec = spec.and(ActivitySpecification.isApproved());
        } else if (isDepartment && currentUser != null) {
            spec = spec.and(ActivitySpecification.isOwnedByOrOrganizedBy(currentUser.getId()));
        } else if (isAdmin) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("status"), 3));
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
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chỉ có thể chỉnh sửa hoạt động đang chờ duyệt hoặc bản nháp.");
        }

        String oldCoverImg = existingActivity.getCoverImage();
        String oldThumbnailImg = existingActivity.getThumbnail();

        activityMapper.updateEntityFromRequest(request, existingActivity);

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
                            "Ngày tổ chức không thuộc Học kỳ nào!"));
            existingActivity.setSemester(newSemester);
        }

        if (request.getOrganizerId() != null &&
                (existingActivity.getOrganizer() == null
                        || !Objects.equals(existingActivity.getOrganizer().getUserId(), request.getOrganizerId()))) {
            Users user = localUserResolver.resolveById(request.getOrganizerId());
            Organizers newOrganizer = getOrCreateOrganizer(user);
            existingActivity.setOrganizer(newOrganizer);
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
        if (request.getBenefits() != null) {
            replaceActivityBenefits(updatedActivity, request.getBenefits());
        }

        activityEventProducer.publishUpdated(updatedActivity);

        ActivityResponse response = activityMapper.toResponse(updatedActivity);
        response.setBenefits(getActivityBenefits(updatedActivity.getId()));
        return response;
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

    private List<BenefitResponse> getActivityBenefits(Long activityId) {
        return benefitRepository.findByActivityId(activityId).stream()
                .map(benefitMapper::toResponse)
                .toList();
    }

    // --- DELETE ---
    @Override
    @Transactional
    public void deleteActivity(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !"));

        Integer currentStatus = activity.getStatus();
        if (currentStatus != null && (currentStatus == 1 || currentStatus == 4)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể xóa hoạt động đã duyệt hoặc hủy.");
        }

        deleteOldImage(activity.getCoverImage());
        deleteOldImage(activity.getThumbnail());

        activityRepository.deleteById(id);

        // ĐÃ FIX: DÙNG KAFKA ĐỂ BÁO CHO NOTIFICATION SERVICE XÓA THÔNG BÁO
        kafkaTemplate.send("iact.activity.deleted", new ActivityDeletedEvent(id));
        activityEventProducer.publishDeleted(id);
        log.info("Đã gửi event Kafka yêu cầu xóa thông báo cho Activity ID: {}", id);
    }

    // --- APPROVE ---
    @Override
    @Transactional
    public void approveActivity(Long id) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0)
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ duyệt được hoạt động Chờ duyệt.");

        activity.setStatus(1);
        Users admin = getCurrentAdmin();
        activity.setHandledBy(admin);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);

        activityEventProducer.publishApproved(savedActivity);
    }

    // --- REJECT ---
    @Override
    @Transactional
    public void rejectActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0)
            throw new AppException(ErrorCode.INVALID_ACTION, "Chỉ từ chối được hoạt động Chờ duyệt.");

        activity.setStatus(2);
        activity.setReason(reason != null && !reason.isBlank() ? reason : "Không có lý do");
        Users admin = getCurrentAdmin();
        activity.setHandledBy(admin);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);

        activityEventProducer.publishRejected(savedActivity);
    }

    // --- CANCEL ---
    @Override
    @Transactional
    public void cancelActivity(Long id, String reason) {
        Activities activity = getActivityForAction(id);
        if (activity.getStatus() != 0 && activity.getStatus() != 1)
            throw new AppException(ErrorCode.INVALID_ACTION, "Lỗi trạng thái");

        activity.setStatus(4);
        activity.setReason(reason != null && !reason.isBlank() ? reason : "Sự cố ngoài ý muốn");
        Users admin = getCurrentAdmin();
        activity.setHandledBy(admin);
        activity.setHandledAt(LocalDateTime.now());
        Activities savedActivity = activityRepository.save(activity);

        activityEventProducer.publishCancelled(savedActivity);
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

    public void deleteOldImage(String oldImg) {
        cloudinaryService.deleteImageByUrl(oldImg);
    }

    public String getQrCodeForActivity(Long activityId) {
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy"));
        return qrCodeService.generateQRCodeBase64(activity.getQrCodeToken(), 300, 300);
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
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy!"));
        int currentStatus = activity.getStatus();
        if (currentStatus == 2 || currentStatus == 4) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Đã bị Từ chối hoặc Hủy!");
        }
        return activity;
    }

    private Users getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        return null;
    }

    // ============ NEW METHODS FOR UC FEATURES ============

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> searchActivities(
            String keyword, Long departmentId, String startDate, String endDate,
            List<Long> categoryIds, String category, String status, Pageable pageable) {

        Specification<Activities> spec = (root, query, cb) -> cb.conjunction();

        // Must be approved for student-facing search
        spec = spec.and(ActivitySpecification.isApproved());

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(ActivitySpecification.containsKeyword(keyword));
        }

        if (departmentId != null) {
            spec = spec.and(ActivitySpecification.hasDepartmentId(departmentId));
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and(ActivitySpecification.hasBenefitCategories(categoryIds));
        }

        if (startDate != null && !startDate.isBlank()) {
            LocalDate start = LocalDate.parse(startDate);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), start.atStartOfDay()));
        }

        if (endDate != null && !endDate.isBlank()) {
            LocalDate end = LocalDate.parse(endDate);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("endDate"), end.atTime(23, 59, 59)));
        }

        if (status != null && !status.equalsIgnoreCase("ALL")) {
            // Map status filter if needed
        }

        Page<Activities> page = activityRepository.findAll(spec, pageable);

        List<ActivityResponse> dtoList = page.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = activityMapper.toResponse(activity);
                    response.setRegisteredCount(
                            (int) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2));
                    return response;
                })
                .collect(Collectors.toList());

        return activityMapper.toPageDTO(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(Long studentId, int limit, Jwt jwt) {
        // If no studentId provided, extract from JWT
        if (studentId == null && jwt != null) {
            String username = jwt.getClaimAsString("preferred_username");
            Users student = userRepository.findByUsername(username).orElse(null);
            if (student != null) {
                studentId = student.getId();
            }
        }

        if (studentId == null) {
            return RecommendationResponse.builder()
                    .activities(List.of())
                    .reasons(List.of())
                    .totalFound(0)
                    .build();
        }

        // Get current semester
        Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
        if (semester == null) {
            return RecommendationResponse.builder()
                    .activities(List.of())
                    .reasons(List.of("Khong co hoc ky hien tai"))
                    .totalFound(0)
                    .build();
        }

        // Simple recommendation: Get approved activities for current semester
        List<Activities> approvedActivities = activityRepository.findApprovedActivitiesForStudent(semester.getId());

        List<RecommendedActivity> recommended = approvedActivities.stream()
                .limit(limit)
                .map(activity -> RecommendedActivity.builder()
                        .id(activity.getId())
                        .title(activity.getTitle())
                        .description(activity.getDescription())
                        .location(activity.getLocation())
                        .startDate(activity.getStartDate() != null ? activity.getStartDate().toString() : null)
                        .endDate(activity.getEndDate() != null ? activity.getEndDate().toString() : null)
                        .maxParticipants(activity.getMaxParticipants())
                        .registeredCount(
                                (int) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2))
                        .matchPercentage(85.0) // Placeholder - real implementation would calculate similarity
                        .matchedReasons(List.of("Hoat dong phu hop voi yeu cau diem ren luyen"))
                        .categoryName(getBenefitCategoryNames(activity.getId()))
                        .departmentName(null)
                        .build())
                .collect(Collectors.toList());

        List<String> reasons = List.of(
                "Cac hoat dong duoc goi y dua tren diem ren luyen con thieu");

        return RecommendationResponse.builder()
                .activities(recommended)
                .reasons(reasons)
                .totalFound(recommended.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ActivityResponse> getActivitiesForRegistration(Long semesterId, Pageable pageable) {
        final Long resolvedSemesterId;
        if (semesterId != null) {
            resolvedSemesterId = semesterId;
        } else {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong co hoc ky"));
            resolvedSemesterId = semester.getId();
        }

        LocalDateTime now = LocalDateTime.now();

        Specification<Activities> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), 1));
            predicates.add(cb.lessThanOrEqualTo(root.get("registrationStart"), now));
            predicates.add(cb.greaterThanOrEqualTo(root.get("registrationEnd"), now));
            predicates.add(cb.equal(root.get("semester").get("id"), resolvedSemesterId));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Activities> page = activityRepository.findAll(spec, pageable);

        List<ActivityResponse> dtoList = page.getContent().stream()
                .map(activity -> {
                    ActivityResponse response = activityMapper.toResponse(activity);
                    response.setRegisteredCount(
                            (int) registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2));
                    return response;
                })
                .collect(Collectors.toList());

        return activityMapper.toPageDTO(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentStatsResponse getDepartmentStatistics(Long departmentId, Long semesterId) {
        Long actualSemesterId = semesterId;
        if (actualSemesterId == null) {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
            if (semester != null) {
                actualSemesterId = semester.getId();
            }
        }

        // Get department name if available
        String departmentName = null;

        // Count activities by status
        List<Activities> activities = activityRepository.findByDepartmentId(departmentId);

        int total = activities.size();
        int pending = (int) activities.stream().filter(a -> a.getStatus() == 0).count();
        int approved = (int) activities.stream().filter(a -> a.getStatus() == 1).count();
        int rejected = (int) activities.stream().filter(a -> a.getStatus() == 2).count();
        int cancelled = (int) activities.stream().filter(a -> a.getStatus() == 4).count();

        return DepartmentStatsResponse.builder()
                .departmentId(departmentId)
                .departmentName(departmentName)
                .semesterId(actualSemesterId)
                .totalActivities(total)
                .pendingActivities(pending)
                .approvedActivities(approved)
                .rejectedActivities(rejected)
                .cancelledActivities(cancelled)
                .totalRegistrations(0) // Placeholder
                .totalAttendances(0)
                .totalCancellations(cancelled)
                .attendanceRate(0.0)
                .totalPointsAwarded(0)
                .uniqueStudentsParticipated(0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStatistics(Long semesterId) {
        Long actualSemesterId = semesterId;
        if (actualSemesterId == null) {
            Semesters semester = semesterRepository.findSemesterByDate(LocalDate.now()).orElse(null);
            if (semester != null) {
                actualSemesterId = semester.getId();
            }
        }

        long totalActivities = activityRepository.count();
        long pending = activityRepository.countByStatus(0);
        long approved = activityRepository.countByStatus(1);
        long rejected = activityRepository.countByStatus(2);

        return SystemStatsResponse.builder()
                .semesterId(actualSemesterId)
                .totalActivities((int) totalActivities)
                .pendingApproval((int) pending)
                .approvedThisSemester((int) approved)
                .rejected((int) rejected)
                .approvalRate(totalActivities > 0 ? (approved * 100.0) / totalActivities : 0)
                .totalRegistrations(0L)
                .totalAttendances(0L)
                .averageAttendanceRate(0.0)
                .build();
    }

    // Temp
    @Override
    public String generateDescription(String prompt) {
        if (prompt == null || prompt.trim().isBlank()) {
            return "Vui long nhap mo ta de iAct tao noi dung hoat dong.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Noi dung hoat dong duoc goi y\n\n");
        sb.append("## Tieu de goi y\n");
        sb.append("- ").append(prompt).append("\n\n");

        sb.append("## Mo ta tom tat\n");
        String[] words = prompt.split("\\s+");
        if (words.length > 3) {
            sb.append("Hoat dong \"");
            for (int i = 0; i < Math.min(5, words.length); i++) {
                sb.append(words[i]).append(" ");
            }
            sb.append("...\" nham mang lai ");
            if (words.length > 5)
                sb.append("kien thuc va ky nang thuc te").append(" cho sinh vien.\n");
            else
                sb.append("trai nghiem hoc tap").append(" cho sinh vien.\n");
        }

        sb.append("\n## Muc tieu\n");
        sb.append("- Giup sinh vien hieu rõ ve chu de: ").append(prompt).append("\n");
        sb.append("- Phat trien ky nang thuc hanh\n");
        sb.append("- Tao co hoi giao luu va hoc hoi\n\n");

        sb.append("## Noi dung chinh\n");
        sb.append("1. Gioi thieu tong quan ve chu de\n");
        sb.append("2. Huong dan va thuc hanh\n");
        sb.append("3. Tha luon va tra loi loi\n");
        sb.append("4. Tong ket va rut kinh nghiem\n\n");

        sb.append("## Ket luan\n");
        sb.append("Hoat dong mang tinh thuc te cao, phu hop voi sinh vien\n");

        log.info("Generated AI description for prompt: {}", prompt);
        return sb.toString();
    }

    private String getBenefitCategoryNames(Long activityId) {
        List<String> categoryNames = benefitRepository.findByActivityId(activityId).stream()
                .map(benefit -> benefit.getCategory())
                .filter(Objects::nonNull)
                .map(category -> category.getName())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return categoryNames.isEmpty() ? null : String.join(", ", categoryNames);
    }
}
