package com.example.activityservice.feature.attendances.service.Impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.AttendanceStatisticsResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;
import com.example.activityservice.feature.attendances.dto.QRVerifyRequest;
import com.example.activityservice.feature.attendances.mapper.AttendanceMapper;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.registration.kafka.RegistrationKafkaProducer;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.activityservice.service.ExcelExportService;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;
    private final ExcelExportService excelExportService;
    private final RegistrationKafkaProducer registrationKafkaProducer;

    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        Users student = getCurrentStudent();
        Registrations registration = registrationRepository.findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Ban chua dang ky hoat dong nay nen khong the diem danh!"));

        if (registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ban da huy dang ky hoat dong nay roi!");
        }

        Activities activity = registration.getActivity();
        String dbQrToken = activity.getQrCodeToken();
        String inputCode = request.getVerifyCode();

        if (dbQrToken == null || inputCode == null || !dbQrToken.equals(inputCode.trim())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ma diem danh khong hop le hoac da het han!");
        }

        Optional<Attendances> existingAttendance = attendanceRepository.findByRegistrationId(registration.getId());

        if (existingAttendance.isEmpty()) {
            Attendances attendance = attendanceMapper.toEntity(request, registration);
            attendance = attendanceRepository.save(attendance);
            registration.setStatus(1);
            registrationRepository.save(registration);

            // Gui Kafka notification
            registrationKafkaProducer.sendCheckInSuccess(
                    student.getId(),
                    activity.getId(),
                    activity.getTitle(),
                    "Hoat dong"
            );

            return attendanceMapper.toResponse(attendance, "Check-in thanh cong!");

        } else {
            Attendances attendance = existingAttendance.get();
            if (attendance.getCheckoutTime() != null) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Ban da hoan tat diem danh ra/vào cho hoat dong nay roi!");
            }

            attendance.setCheckoutTime(LocalDateTime.now());
            attendance = attendanceRepository.save(attendance);

            return attendanceMapper.toResponse(attendance, "Check-out thanh cong!");
        }
    }

    // ============ NEW METHODS FOR UC FEATURES ============

    @Override
    @Transactional(readOnly = true)
    public PageDTO<AttendanceResponse> getAttendancesBySession(Long activityId, Long sessionId, Pageable pageable) {
        List<Registrations> registrations = registrationRepository.findAllByActivityId(activityId);
        
        // Filter registrations that attended this session
        List<Attendances> attendances = attendanceRepository.findByRegistrationIn(registrations);

        List<AttendanceResponse> responses = attendances.stream()
                .map(a -> attendanceMapper.toResponse(a, null))
                .collect(Collectors.toList());

        // Simple pagination simulation
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<AttendanceResponse> pageContent = start >= responses.size() 
                ? List.of() 
                : responses.subList(start, end);

        return PageDTO.<AttendanceResponse>builder()
                .pageNumber(pageable.getPageNumber() + 1)
                .totalPage((int) Math.ceil((double) responses.size() / pageable.getPageSize()))
                .totalRows(responses.size())
                .data(pageContent)
                .build();
    }

    @Override
    @Transactional
    public AttendanceResponse verifyAndCheckIn(QRVerifyRequest request) {
        // Parse QR data and find registration
        Registrations registration = null;
        
        if (request.getRegistrationId() != null) {
            registration = registrationRepository.findById(request.getRegistrationId()).orElse(null);
        }

        if (registration == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay dang ky");
        }

        // Check if already attended
        Optional<Attendances> existing = attendanceRepository.findByRegistrationId(registration.getId());
        if (existing.isPresent() && existing.get().getCheckoutTime() != null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Sinh vien da diem danh roi");
        }

        // Create or update attendance
        Attendances attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setCheckoutTime(LocalDateTime.now());
        } else {
            attendance = Attendances.builder()
                    .registration(registration)
                    .checkinTime(LocalDateTime.now())
                    .method(1) // QR method
                    .build();
        }

        attendance = attendanceRepository.save(attendance);

        // Update registration status
        registration.setStatus(1);
        registrationRepository.save(registration);

        // Gui Kafka notification
        registrationKafkaProducer.sendCheckInSuccess(
                registration.getStudent().getId(),
                registration.getActivity().getId(),
                registration.getActivity().getTitle(),
                "Quet QR"
        );

        return attendanceMapper.toResponse(attendance, "Diem danh thanh cong!");
    }

    @Override
    public void exportAttendanceToExcel(Long activityId, Long sessionId, OutputStream outputStream) throws Exception {
        List<Registrations> registrations = registrationRepository.findAllByActivityId(activityId);
        List<Attendances> attendances = attendanceRepository.findByRegistrationIn(registrations);

        String[] headers = {"STT", "MSSV", "Ho Ten", "Lop", "Gio Diem Danh", "Gio Diem Ra", "Trang Thai"};
        java.util.concurrent.atomic.AtomicInteger stt = new java.util.concurrent.atomic.AtomicInteger(1);

        excelExportService.export(
                "Danh_sach_Diem_Danh",
                headers,
                attendances,
                (attendance) -> {
                    Registrations reg = attendance.getRegistration();
                    Users student = reg.getStudent();
                    
                    return new Object[]{
                            stt.getAndIncrement(),
                            student.getStudentCode() != null ? student.getStudentCode() : "",
                            student.getFullName() != null ? student.getFullName() : student.getUsername(),
                            "", // Lop - can join with profile
                            attendance.getCheckinTime() != null ? attendance.getCheckinTime().toString() : "",
                            attendance.getCheckoutTime() != null ? attendance.getCheckoutTime().toString() : "",
                            attendance.getCheckoutTime() != null ? "Da diem danh" : "Chua diem ra"
                    };
                },
                outputStream
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getStatistics(Long activityId, Long sessionId) {
        List<Registrations> registrations = registrationRepository.findAllByActivityId(activityId);
        List<Attendances> attendances = attendanceRepository.findByRegistrationIn(registrations);

        int totalRegistrations = registrations.size();
        int totalAttendances = (int) attendances.stream()
                .filter(a -> a.getCheckinTime() != null)
                .count();
        int totalAbsences = totalRegistrations - totalAttendances;
        double attendanceRate = totalRegistrations > 0 
                ? (totalAttendances * 100.0) / totalRegistrations 
                : 0;

        return AttendanceStatisticsResponse.builder()
                .activityId(activityId)
                .sessionId(sessionId)
                .totalRegistrations(totalRegistrations)
                .totalAttendances(totalAttendances)
                .totalAbsences(totalAbsences)
                .attendanceRate(Math.round(attendanceRate * 10.0) / 10.0)
                .presentMale(0)  // Placeholder
                .presentFemale(0)
                .absentMale(0)
                .absentFemale(0)
                .build();
    }
}
