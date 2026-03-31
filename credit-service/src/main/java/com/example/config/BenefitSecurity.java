package com.example.config;

import com.example.common.entity.Activities;
import com.example.common.repository.LocalActivityRepository;
import com.example.feature.benefits.repository.BenefitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("benefitSecurity")
@RequiredArgsConstructor
@Slf4j
public class BenefitSecurity {

    private final BenefitRepository benefitRepository;
    private final LocalActivityRepository localActivityRepository;

    @Transactional(readOnly = true)
    public boolean hasActivityPermission(Authentication authentication, Long activityId) {
        if (isAdmin(authentication)) return true;

        return localActivityRepository.findById(activityId)
                .map(activity -> isOwner(authentication, activity))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasBenefitPermission(Authentication authentication, Long benefitId) {
        if (isAdmin(authentication)) return true;

        return benefitRepository.findById(benefitId)
                .map(benefit -> isOwner(authentication, benefit.getActivity()))
                .orElse(false);
    }

    // --- Helper ---
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isOwner(Authentication authentication, Activities activity) {
        String currentUsername = authentication.getName();
        if (currentUsername == null || activity == null) {
            log.warn("Không thể xác định quyền: Username hoặc Activity bị null");
            return false;
        }

        // Kiểm tra xem User đăng nhập có trùng với User tạo Hoạt động không
        boolean isCreator = currentUsername.equals(activity.getCreatedByUsername());

        if (!isCreator) {
            log.warn("User {} cố gắng can thiệp vào hoạt động của user {}", currentUsername, activity.getCreatedByUsername());
        }

        return isCreator;
    }
}