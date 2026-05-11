package com.example.activityservice.feature.benefits.service;

import com.example.activityservice.feature.benefits.dto.BenefitRequest;
import com.example.activityservice.feature.benefits.dto.BenefitResponse;

import java.util.List;

public interface BenefitService {
    BenefitResponse createBenefit(BenefitRequest request);
    List<BenefitResponse> getBenefitsByActivityId(Long activityId);
    BenefitResponse getBenefitById(Long id);
    BenefitResponse updateBenefit(Long id, BenefitRequest request);
    void deleteBenefit(Long id);
}