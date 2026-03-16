package com.example.feature.attendances.service.Impl;

import com.example.common.entity.Users;
import com.example.common.repository.LocalUserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.activities.model.Activities;
import com.example.feature.attendances.dto.AttendanceResponse;
import com.example.feature.attendances.dto.CheckInRequest;
import com.example.feature.attendances.mapper.AttendanceMapper;
import com.example.feature.attendances.model.Attendances;
import com.example.feature.attendances.repository.AttendanceRepository;
import com.example.feature.attendances.service.AttendanceService;
import com.example.feature.registration.model.Registrations;
import com.example.feature.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final LocalUserRepository userRepository;
    private final AttendanceMapper attendanceMapper;

    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        Users student = getCurrentStudent();
        // 1. Kiểm tra trạng thái đăng ký
        Registrations registration = registrationRepository.findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Bạn chưa đăng ký hoạt động này nên không thể điểm danh!"));

        if (registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hủy đăng ký hoạt động này rồi!");
        }

        // 2. Kiểm tra mã QR
        Activities activity = registration.getActivity();
        String dbQrToken = activity.getQrCodeToken();
        String inputCode = request.getVerifyCode();

        if (dbQrToken == null || inputCode == null || !dbQrToken.equals(inputCode.trim())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Mã điểm danh không hợp lệ hoặc đã hết hạn!");
        }

        // 3. LOGIC ĐIỂM DANH
        Optional<Attendances> existingAttendance = attendanceRepository.findByRegistrationId(registration.getId());

        if (existingAttendance.isEmpty()) {
            // Check-in
            Attendances attendance = attendanceMapper.toEntity(request, registration);
            attendance = attendanceRepository.save(attendance);
            registration.setStatus(1);
            registrationRepository.save(registration);

            return attendanceMapper.toResponse(attendance, "Check-in thành công!");

        } else {
            // Check-out
            Attendances attendance = existingAttendance.get();
            if (attendance.getCheckoutTime() != null) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hoàn tất điểm danh ra/vào cho hoạt động này rồi!");
            }

            attendance.setCheckoutTime(LocalDateTime.now());
            attendance = attendanceRepository.save(attendance);

            return attendanceMapper.toResponse(attendance, "Check-out thành công!");
        }
    }
}