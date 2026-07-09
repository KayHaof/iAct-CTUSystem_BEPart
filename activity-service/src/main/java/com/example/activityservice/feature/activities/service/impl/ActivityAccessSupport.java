package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ActivityAccessSupport {

    private final UserRepository userRepository;

    public Users getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    public Long currentStudentDepartmentId() {
        Users currentUser = getCurrentUserOrNull();
        return currentUser != null ? currentUser.getDepartmentId() : null;
    }

    public boolean isCurrentStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
        boolean isDepartment = authentication.getAuthorities().stream()
                .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_DEPARTMENT"));
        return !isAdmin && !isDepartment;
    }

    public boolean isVisibleToStudent(Activities activity, Users student) {
        if (Boolean.TRUE.equals(activity.getIsExternal())) {
            return true;
        }
        if (!Boolean.TRUE.equals(activity.getIsFaculty())) {
            return true;
        }
        return student != null
                && student.getDepartmentId() != null
                && Objects.equals(student.getDepartmentId(), activity.getDepartmentId());
    }
}
