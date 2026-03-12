package com.example.config;

import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.registration.model.Registrations; // Ní nhớ import đúng entity Registrations của ní
import com.example.feature.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("activitySecurity")
@RequiredArgsConstructor
public class ActivitySecurity {
    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public boolean hasActivityPermission(Authentication authentication, Long activityId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        return activityRepository.findById(activityId)
                .map(activity -> isOwner(authentication, activity))
                .orElse(false);
    }

    // Check quyền dựa trên ID của Đơn đăng ký (Registration)
    @Transactional(readOnly = true)
    public boolean hasRegistrationPermission(Authentication authentication, Long registrationId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        // Tìm đơn đăng ký -> Rút ra Hoạt động (Activity)
        return registrationRepository.findById(registrationId)
                // Giả định entity Registrations có phương thức getActivity()
                .map(registration -> isOwner(authentication, registration.getActivity()))
                .orElse(false);
    }

    private boolean isOwner(Authentication authentication, Activities activity) {
        String currentUsername = authentication.getName();

        if (currentUsername == null) {
            return false;
        }

        if (activity.getCreatedBy() != null
                && currentUsername.equals(activity.getCreatedBy().getUsername())) {
            return true;
        }

        return activity.getOrganizer() != null
                && activity.getOrganizer().getUser() != null
                && currentUsername.equals(activity.getOrganizer().getUser().getUsername());
    }
}