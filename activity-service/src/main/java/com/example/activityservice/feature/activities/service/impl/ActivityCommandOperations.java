package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.dto.ActivityRequest;
import com.example.activityservice.feature.activities.dto.ActivityResponse;
import com.example.activityservice.feature.activities.kafka.ActivityEventProducer;
import com.example.activityservice.feature.activities.mapper.ActivityMapper;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
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
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.semesters.repository.SemesterRepository;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.service.LocalUserResolver;
import com.example.activityservice.service.CloudinaryService;
import com.example.activityservice.service.QRCodeService;
import com.example.event.ActivityDeletedEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityCommandOperations {

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
    private final ActivityResponseAssembler responseAssembler;
    private final ActivityCacheService activityCacheService;

    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        boolean isDraft = Integer.valueOf(3).equals(request.getStatus());
        Semesters semester = resolveSemesterForCreate(request, isDraft);
        Organizers organizer = resolveOrganizer(request);

        Activities activity = activityMapper.toEntity(request, organizer);
        if (semester != null) {
            activity.setSemester(semester);
        }
        applyCurrentCreator(activity);
        applySchedules(activity, request);

        Activities savedActivity = activityRepository.save(activity);
        List<BenefitResponse> savedBenefits = replaceActivityBenefits(savedActivity, request.getBenefits());

        if (savedActivity.getStatus() != 3) {
            log.info("Hoat dong da duoc tao va gui duyet thanh cong!");
        } else {
            log.info("Ban nhap hoat dong da duoc luu thanh cong!");
        }

        ActivityResponse response = responseAssembler.toResponse(savedActivity);
        response.setBenefits(savedBenefits);
        activityEventProducer.publishCreated(savedActivity);
        if (savedActivity.getStatus() != 3) {
            activityEventProducer.publishSubmitted(savedActivity);
        }
        activityCacheService.evictActivityListCaches();
        return response;
    }

    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activities existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay hoat dong!"));

        if (existingActivity.getStatus() != 0 && existingActivity.getStatus() != 3) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chi co the chinh sua hoat dong dang cho duyet hoac ban nhap.");
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
                            "Ngay to chuc khong thuoc hoc ky nao!"));
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

        ActivityResponse response = responseAssembler.toResponse(updatedActivity);
        response.setBenefits(responseAssembler.getActivityBenefits(updatedActivity.getId()));
        activityCacheService.evictActivityListCaches();
        return response;
    }

    @Transactional
    public void deleteActivity(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Activity not found !"));

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
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay"));
        return qrCodeService.generateQRCodeBase64(activity.getQrCodeToken(), 300, 300);
    }

    public void deleteOldImage(String oldImg) {
        cloudinaryService.deleteImageByUrl(oldImg);
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

    private Organizers resolveOrganizer(ActivityRequest request) {
        if (request.getOrganizerId() == null) {
            return null;
        }
        Users user = localUserResolver.resolveById(request.getOrganizerId());
        return getOrCreateOrganizer(user);
    }

    private void applyCurrentCreator(Activities activity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String currentUsername = authentication.getName();
            Users currentUser = localUserResolver.resolveByUsername(currentUsername);

            activity.setCreatedBy(currentUser);
            activity.setCreatedByUsername(currentUsername);
            activity.setDepartmentId(currentUser.getDepartmentId());
        }
    }

    private void applySchedules(Activities activity, ActivityRequest request) {
        if (request.getSchedules() != null && !request.getSchedules().isEmpty()) {
            List<ActivitySchedule> schedulesList = scheduleMapper.toEntityList(request.getSchedules());
            schedulesList.forEach(schedule -> schedule.setActivity(activity));
            activity.setSchedules(schedulesList);
        }
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
}
