package com.example.feature.activities.service.impl;

import com.example.common.dto.BenefitDto;
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
import com.example.feature.activities.mapper.ActivityMapper;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.activities.service.ActivityService;
import com.example.feature.organizers.model.Organizers;
import com.example.feature.organizers.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final LocalSemesterRepository semesterRepository;
    private final OrganizerRepository organizerRepository;
    private final LocalCategoryRepository categoryRepository;
    private final LocalUserRepository userRepository;
    private final ActivityMapper activityMapper;

    // --- CREATE ---
    @Override
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        Semesters semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Semester not found !"));

        Organizers organizer = organizerRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Organizer not found !"));

        Activities activity = activityMapper.toEntity(request, semester, organizer);

        // Logic lấy User
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String currentUsername = authentication.getName();
            Users currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            activity.setCreatedBy(currentUser);
        }

        List<Benefits> benefitsList = processBenefits(request.getBenefits(), activity);
        if (!benefitsList.isEmpty()) {
            activity.setBenefits(benefitsList);
        }

        Activities savedActivity = activityRepository.save(activity);
        return activityMapper.toResponse(savedActivity);
    }

    // --- READ ---
    @Override
    public ActivityResponse getActivityById(Long id) {
        Activities activity = activityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "The activity with id = " + id + " not found !"));
        return activityMapper.toResponse(activity);
    }

    // --- ALL ---
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
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "The activity with id = " + id + " not found !"));

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
        return activityMapper.toResponse(updatedActivity);
    }

    // --- DELETE ---
    @Override
    @Transactional
    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "The activity with id = " + id + " not found !");
        }
        activityRepository.deleteById(id);
    }

    private List<Benefits> processBenefits(List<BenefitDto> benefitDtos, Activities activity) {
        if (benefitDtos == null || benefitDtos.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return benefitDtos.stream().map(dto -> {
            Benefits benefit = activityMapper.toBenefitEntity(dto);
            benefit.setActivity(activity);

            if (dto.getCategoryId() != null) {
                Categories category = categoryRepository.findById(dto.getCategoryId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED));
                benefit.setCategory(category);
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

        if (newStatus == 2) {
            String reason = request.getRejectReason() != null ? request.getRejectReason() : "No reason provided";
            System.out.println("LOG: Rejected Activity " + id + ". Reason: " + reason);
            // notificationService.sendNotification(organizerId, "Hoạt động bị từ chối: " + reason);
        }
        else if (newStatus == 3) {
            String reason = request.getCancelReason() != null ? request.getCancelReason() : "Emergency cancellation";
            System.out.println("LOG: Cancelled Activity " + id + ". Reason: " + reason);

            // TODO: Quan trọng! Cần gửi thông báo cho TẤT CẢ sinh viên đã đăng ký
            // List<User> participants = registerRepository.findUsersByActivity(id);
            // notificationService.sendBroadCast(participants, "Hoạt động đã bị hủy vì sự cố: " + reason);
        }

        Activities savedActivity = activityRepository.save(activity);
        return activityMapper.toResponse(savedActivity);
    }
}