package com.example.activityservice.feature.activities.service.impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
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
        Authentication authentication = currentAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    public Users requireCurrentUser() {
        Users currentUser = getCurrentUserOrNull();
        if (currentUser == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Không xác định được tài khoản hiện tại.");
        }
        return currentUser;
    }

    public Users requireCurrentDepartmentUser() {
        if (!isCurrentDepartment()) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ tài khoản Khoa/Đơn vị mới được truy cập dữ liệu này.");
        }
        Users currentUser = requireCurrentUser();
        if (currentUser.getDepartmentId() == null) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản Khoa/Đơn vị chưa được gắn đơn vị.");
        }
        return currentUser;
    }

    public Long requireCurrentDepartmentId() {
        return requireCurrentDepartmentUser().getDepartmentId();
    }

    public Long currentStudentDepartmentId() {
        Users currentUser = getCurrentUserOrNull();
        return currentUser != null ? currentUser.getDepartmentId() : null;
    }

    public boolean isCurrentAdmin() {
        return hasCurrentAuthority("ROLE_ADMIN");
    }

    public boolean isCurrentDepartment() {
        return hasCurrentAuthority("ROLE_DEPARTMENT");
    }

    public boolean isCurrentStudent() {
        Authentication authentication = currentAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        boolean isAdmin = isCurrentAdmin();
        boolean isDepartment = isCurrentDepartment();
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

    public boolean isCreatedBy(Activities activity, Users user) {
        return activity != null
                && user != null
                && activity.getCreatedBy() != null
                && Objects.equals(activity.getCreatedBy().getId(), user.getId());
    }

    public boolean canCurrentDepartmentManageActivity(Activities activity) {
        if (!isCurrentDepartment() || activity == null || activity.getDepartmentId() == null) {
            return false;
        }
        Users currentUser = getCurrentUserOrNull();
        return currentUser != null
                && currentUser.getDepartmentId() != null
                && Objects.equals(currentUser.getDepartmentId(), activity.getDepartmentId());
    }

    public void ensureCurrentDepartmentCanManageActivity(Activities activity) {
        if (canCurrentDepartmentManageActivity(activity)) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền truy cập hoạt động ngoài đơn vị của mình.");
    }

    public void ensureCurrentDepartmentCanRead(Activities activity) {
        if (!isCurrentDepartment()) {
            return;
        }
        ensureCurrentDepartmentCanManageActivity(activity);
    }

    public void ensureCurrentUserCanManageActivity(Activities activity) {
        if (isCurrentAdmin()) {
            return;
        }
        Users currentUser = getCurrentUserOrNull();
        if (isCreatedBy(activity, currentUser) || canCurrentDepartmentManageActivity(activity)) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền thao tác trên hoạt động này.");
    }

    private boolean hasCurrentAuthority(String authority) {
        Authentication authentication = currentAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(auth -> Objects.equals(auth.getAuthority(), authority));
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
