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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        Registrations registration = resolveCurrentStudentRegistration(request);

        return recordAttendance(
                registration,
                request.getMethod(),
                request.getLatitude(),
                request.getLongitude(),
                "Ma diem danh",
                "Check-in thanh cong!",
                "Ban da duoc ghi nhan tham gia truoc do!"
        );
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(CheckInRequest request) {
        Registrations registration = resolveCurrentStudentRegistration(request);

        return recordCheckout(
                registration,
                "Check-out thanh cong!",
                "Ban da duoc ghi nhan check-out truoc do!"
        );
    }

    private Registrations resolveCurrentStudentRegistration(CheckInRequest request) {
        Users student = getCurrentStudent();
        Registrations registration = registrationRepository.findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Ban chua dang ky hoat dong nay nen khong the diem danh!"));

        if (registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ban da huy dang ky hoat dong nay roi!");
        }

        validateActivityQrToken(registration.getActivity(), request.getVerifyCode());
        return registration;
    }

    private void validateActivityQrToken(Activities activity, String inputCode) {
        String dbQrToken = activity.getQrCodeToken();

        if (dbQrToken == null || inputCode == null || !dbQrToken.equals(inputCode.trim())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ma diem danh khong hop le hoac da het han!");
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
        Registrations registration = resolveRegistrationFromQrRequest(request);

        if (request.getActivityId() != null
                && !request.getActivityId().equals(registration.getActivity().getId())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ma QR khong thuoc hoat dong nay");
        }

        if (registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Dang ky nay da bi huy");
        }

        if ("CHECK_OUT".equalsIgnoreCase(request.getAction())) {
            return recordCheckout(
                    registration,
                    "Check-out thanh cong!",
                    "Sinh vien da duoc ghi nhan check-out truoc do!"
            );
        }

        return recordAttendance(
                registration,
                1,
                null,
                null,
                "Quet QR sinh vien",
                "Diem danh thanh cong!",
                "Sinh vien da duoc ghi nhan tham gia truoc do!"
        );
    }

    private Registrations resolveRegistrationFromQrRequest(QRVerifyRequest request) {
        Long registrationId = request.getRegistrationId();

        if (registrationId == null) {
            registrationId = parseRegistrationId(request.getQrData());
        }

        if (registrationId == null) {
            registrationId = parseRegistrationId(request.getVerifyCode());
        }

        if (registrationId == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ma QR khong hop le");
        }

        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Khong tim thay dang ky"));
    }

    private Long parseRegistrationId(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        String value = rawValue.trim();
        String upperValue = value.toUpperCase();
        if (upperValue.startsWith("CK")) {
            return parseLongSafely(upperValue.substring(2));
        }

        if (value.matches("\\d+")) {
            return parseLongSafely(value);
        }

        try {
            JsonNode node = objectMapper.readTree(value);
            if (node.hasNonNull("regId")) {
                return node.get("regId").asLong();
            }
            if (node.hasNonNull("registrationId")) {
                return node.get("registrationId").asLong();
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private Long parseLongSafely(String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AttendanceResponse recordAttendance(
            Registrations registration,
            Integer method,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            String notificationSource,
            String successMessage,
            String alreadyCheckedMessage
    ) {
        Optional<Attendances> existing = attendanceRepository.findByRegistrationId(registration.getId());
        if (existing.isPresent()) {
            Attendances attendance = existing.get();
            if (registration.getStatus() != 1) {
                registration.setStatus(1);
                registrationRepository.save(registration);
            }
            return attendanceMapper.toResponse(attendance, alreadyCheckedMessage);
        }

        Attendances attendance = Attendances.builder()
                .registration(registration)
                .checkinTime(LocalDateTime.now())
                .method(method != null ? method : 1)
                .latitude(latitude)
                .longitude(longitude)
                .build();
        attendance = attendanceRepository.save(attendance);

        registration.setStatus(1);
        registrationRepository.save(registration);

        registrationKafkaProducer.sendCheckInSuccess(
                registration.getStudent().getId(),
                registration.getActivity().getId(),
                registration.getActivity().getTitle(),
                notificationSource
        );

        return attendanceMapper.toResponse(attendance, successMessage);
    }

    private AttendanceResponse recordCheckout(
            Registrations registration,
            String successMessage,
            String alreadyCheckedMessage
    ) {
        Attendances attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Chua co thong tin check-in nen khong the check-out!"));

        if (attendance.getCheckinTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Chua co thong tin check-in nen khong the check-out!");
        }

        if (attendance.getCheckoutTime() != null) {
            return attendanceMapper.toResponse(attendance, alreadyCheckedMessage);
        }

        attendance.setCheckoutTime(LocalDateTime.now());
        attendance = attendanceRepository.save(attendance);

        if (registration.getStatus() != 1) {
            registration.setStatus(1);
            registrationRepository.save(registration);
        }

        return attendanceMapper.toResponse(attendance, successMessage);
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
