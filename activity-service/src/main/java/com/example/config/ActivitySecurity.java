package com.example.config;

import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("activitySecurity")
@RequiredArgsConstructor
public class ActivitySecurity {

    private final ActivityRepository activityRepository;

    @Transactional(readOnly = true)
    public boolean hasActivityPermission(Authentication authentication, Long activityId) {


        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        System.out.println(">> Check role admin input = " + isAdmin );
        if (isAdmin) {
            return true;
        }

        return activityRepository.findById(activityId)
                .map(activity -> isOwner(authentication, activity))
                .orElse(false);
    }

    private boolean isOwner(Authentication authentication, Activities activity) {
        String currentKeycloakId = authentication.getName();

        if (activity.getOrganizer() != null && activity.getOrganizer().getUser() != null) {
            String ownerKeycloakId = activity.getOrganizer().getUser().getKeycloakId();
            return currentKeycloakId.equals(ownerKeycloakId);
        }

        return false;
    }
}