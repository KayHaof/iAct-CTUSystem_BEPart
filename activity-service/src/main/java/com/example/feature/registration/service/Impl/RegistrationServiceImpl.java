package com.example.feature.registration.service.Impl;

import com.example.common.entity.Users;
import com.example.common.repository.LocalUserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.mapper.RegistrationMapper;
import com.example.feature.registration.model.Registrations;
import com.example.feature.registration.repository.RegistrationRepository;
import com.example.feature.registration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final LocalUserRepository userRepository;
    private final RegistrationMapper registrationMapper;

    // --- HÀM DÙNG CHUNG: Lấy sinh viên đang đăng nhập ---
    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    public RegistrationResponse getMyStatusByActivity(Long activityId) {
        Users student = getCurrentStudent();
        return registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .map(registrationMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        Users student = getCurrentStudent();

        Activities activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Hoạt động không tồn tại"));

        // 1. Kiểm tra trạng thái và thời gian mở đăng ký
        if (activity.getStatus() != 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này hiện chưa mở đăng ký.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistrationStart()) || now.isAfter(activity.getRegistrationEnd())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Rất tiếc, đã hết hoặc chưa tới thời hạn đăng ký.");
        }

        // 2. Lấy record đăng ký cũ (nếu có)
        Registrations existingReg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activity.getId())
                .orElse(null);

        if (existingReg != null) {
            if (existingReg.getStatus() == 0 || existingReg.getStatus() == 1) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã đăng ký hoạt động này rồi nha!");
            }
            // Nếu status == 2 (Đã hủy), cho phép đi tiếp để "tái chế"
        }

        // 3. ĐẾM SỐ LƯỢNG TRỰC TIẾP TỪ DATABASE (Chuẩn với thiết kế của ní)
        long currentCount = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2); // 2 là Đã hủy
        if (currentCount >= activity.getMaxParticipants()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này đã full chỗ mất rồi!");
        }

        // 4. Xử lý lưu Registration (Thêm mới hoặc Cập nhật lại cái đã hủy)
        Registrations regToSave;
        if (existingReg != null && existingReg.getStatus() == 2) {
            // Đăng ký lại: Hồi sinh record cũ, không tạo thêm dòng mới rác DB
            regToSave = existingReg;
            regToSave.setStatus(0);
            regToSave.setCancelReason(null); // Xóa lý do hủy cũ
            regToSave.setRegisteredAt(LocalDateTime.now()); // Cập nhật lại giờ đăng ký mới
        } else {
            // Đăng ký mới hoàn toàn
            regToSave = new Registrations();
            regToSave.setStudent(student);
            regToSave.setActivity(activity);
            regToSave.setRegisteredAt(LocalDateTime.now());
            regToSave.setStatus(0);
        }

        return registrationMapper.toResponse(registrationRepository.save(regToSave));
    }

    @Override
    @Transactional
    public RegistrationResponse cancelByActivityId(Long activityId, String reason) {
        Users student = getCurrentStudent();

        Registrations reg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Ní chưa đăng ký hoạt động này nên không hủy được!"));

        return this.processCancellation(reg, reason);
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long regId, String reason) {
        Registrations reg = registrationRepository.findById(regId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED));

        return this.processCancellation(reg, reason);
    }

    // --- HÀM DÙNG CHUNG: Xử lý logic Hủy đăng ký ---
    private RegistrationResponse processCancellation(Registrations reg, String reason) {
        if (reg.getStatus() == 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không được hủy khi đã điểm danh!");
        }
        if (reg.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hủy đăng ký trước đó rồi!");
        }

        // Cập nhật trạng thái thành Đã hủy (Khi Hủy, record mang status 2, hàm COUNT ở trên sẽ tự động giảm đi 1)
        reg.setStatus(2);
        reg.setCancelReason(reason);

        return registrationMapper.toResponse(registrationRepository.save(reg));
    }

    @Override
    public List<RegistrationResponse> getByActivity(Long activityId) {
        return registrationRepository.findAllByActivityId(activityId).stream()
                .map(registrationMapper::toResponse)
                .collect(Collectors.toList());
    }
}