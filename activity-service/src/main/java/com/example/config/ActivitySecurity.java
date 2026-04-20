package com.example.config;

import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component("activitySecurity")
@RequiredArgsConstructor
public class ActivitySecurity {
    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public boolean hasActivityPermission(Authentication authentication, Long activityId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        return activityRepository.findById(activityId)
                .map(activity -> isOwner(authentication, activity))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasRegistrationPermission(Authentication authentication, Long registrationId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));

        if (isAdmin) {
            return true;
        }

        return registrationRepository.findById(registrationId)
                .map(registration -> isOwner(authentication, registration.getActivity()))
                .orElse(false);
    }

    private boolean isOwner(Authentication authentication, Activities activity) {
        String currentUsername = authentication.getName();

        if (currentUsername == null) {
            return false;
        }

        // 1. Kiểm tra người tạo hoạt động
        if (activity.getCreatedByUsername() != null
                && currentUsername.equals(activity.getCreatedByUsername())) {
            return true;
        }

        // 2. Kiểm tra người tổ chức (Organizer)
        if (activity.getOrganizer() != null && activity.getOrganizer().getId() != null) {

            // TRƯỜNG HỢP A: Nếu authentication.getName() của ní trả về ID (ví dụ chuỗi "105" thay vì "admin")
            // thì ní có thể mở comment dòng này để so sánh:
            // return currentUsername.equals(String.valueOf(activity.getOrganizer().getId()));

            // TRƯỜNG HỢP B (Thực tế nhất): Ní phải lấy được ID của user đang đăng nhập từ Token hoặc gọi qua DB Profile.
            // Để code không bị lỗi lúc này, tui tạm thời để false nếu không rớt vào case người tạo.
            // Khi rảnh, ní nên add thêm cột 'organizerUsername' vào bảng organizers để check cho lẹ giống thằng createdBy.
        }

        return false;
    }
}