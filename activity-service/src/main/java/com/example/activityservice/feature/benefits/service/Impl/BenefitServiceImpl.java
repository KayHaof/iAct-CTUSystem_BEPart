package com.example.activityservice.feature.benefits.service.Impl;


import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.benefits.dto.BenefitRequest;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;
import com.example.activityservice.feature.benefits.mapper.BenefitMapper;
import com.example.activityservice.feature.benefits.model.Benefits;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.activityservice.feature.benefits.repository.BenefitRepository;
import com.example.activityservice.feature.benefits.service.BenefitService;
import com.example.activityservice.feature.benefits.service.BenefitValidationService;
import com.example.activityservice.feature.points.service.PointCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenefitServiceImpl implements BenefitService {

    private final BenefitRepository benefitRepository;
    private final ActivityRepository localActivityRepository;
    private final BenefitMapper benefitMapper;
    private final BenefitValidationService benefitValidationService;
    private final PointCacheService pointCacheService;
    private final ActivityCacheService activityCacheService;

    @Override
    @Transactional
    public BenefitResponse createBenefit(BenefitRequest request) {
        if (request.getActivityId() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn hoạt động cho quyền lợi!");
        }
        Activities activity = localActivityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy thông tin hoạt động trong hệ thống!"));

        Categories category = benefitValidationService.validateAndGetCategory(
                request.getCategoryId(), request.getPoint(), request.getType());

        Benefits benefit = Benefits.builder()
                .activity(activity)
                .category(category)
                .type(request.getType())
                .point(request.getPoint())
                .build();

        Benefits saved = benefitRepository.save(benefit);
        evictSemesterPointCaches(saved);
        activityCacheService.evictActivityListCaches();
        return benefitMapper.toResponse(saved);
    }

    @Override
    public List<BenefitResponse> getBenefitsByActivityId(Long activityId) {
        return benefitRepository.findByActivityId(activityId).stream()
                .map(benefitMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BenefitResponse getBenefitById(Long id) {
        Benefits benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy quyền lợi!"));
        return benefitMapper.toResponse(benefit);
    }

    @Override
    @Transactional
    public BenefitResponse updateBenefit(Long id, BenefitRequest request) {
        log.info("Đang cập nhật quyền lợi ID: {}", id);

        Benefits benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy quyền lợi để cập nhật!"));

        if (request.getActivityId() != null && !request.getActivityId().equals(benefit.getActivity().getId())) {
            Activities activity = localActivityRepository.findById(request.getActivityId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));
            benefit.setActivity(activity);
        }

        Categories category = benefitValidationService.validateAndGetCategory(
                request.getCategoryId(), request.getPoint(), request.getType());
        benefit.setCategory(category);

        benefit.setType(request.getType());
        benefit.setPoint(request.getPoint());

        Benefits updated = benefitRepository.save(benefit);
        evictSemesterPointCaches(updated);
        activityCacheService.evictActivityListCaches();
        log.info("Cập nhật quyền lợi thành công: {}", updated.getId());

        return benefitMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBenefit(Long id) {
        Benefits benefit = benefitRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay quyen loi de xoa!"));
        log.info("Đang xóa quyền lợi ID: {}", id);
        if (!benefitRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy quyền lợi để xóa!");
        }
        benefitRepository.deleteById(id);
        evictSemesterPointCaches(benefit);
        activityCacheService.evictActivityListCaches();
        log.info("Xóa quyền lợi thành công: {}", id);
    }
    private void evictSemesterPointCaches(Benefits benefit) {
        Long semesterId = benefit.getActivity() != null && benefit.getActivity().getSemester() != null
                ? benefit.getActivity().getSemester().getId()
                : null;
        pointCacheService.evictSemesterPointCaches(semesterId);
    }
}
