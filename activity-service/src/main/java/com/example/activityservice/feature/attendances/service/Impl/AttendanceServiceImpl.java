package com.example.activityservice.feature.attendances.service.Impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import com.example.activityservice.feature.attendances.dto.AttendanceStatisticsResponse;
import com.example.activityservice.feature.attendances.dto.CheckInRequest;
import com.example.activityservice.feature.attendances.dto.FaceCheckInRequest;
import com.example.activityservice.feature.attendances.dto.FaceCheckInResponse;
import com.example.activityservice.feature.attendances.mapper.AttendanceMapper;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.attendances.model.FaceCheckInAttempt;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
import com.example.activityservice.feature.attendances.repository.FaceCheckInAttemptRepository;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.activityservice.feature.face_embedding.ai.AiFaceVerificationClient;
import com.example.activityservice.feature.face_embedding.ai.FaceVerificationResult;
import com.example.activityservice.feature.face_embedding.model.StudentFaceEmbeddingProjection;
import com.example.activityservice.feature.face_embedding.service.StudentFaceEmbeddingProjectionService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private static final int FACE_CHECK_IN_MAX_ATTEMPTS = 5;
    private static final long MAX_LIVE_IMAGE_BYTES = 8L * 1024L * 1024L;

    private final AttendanceRepository attendanceRepository;
    private final FaceCheckInAttemptRepository faceCheckInAttemptRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;
    private final ExcelExportService excelExportService;
    private final RegistrationKafkaProducer registrationKafkaProducer;
    private final StudentFaceEmbeddingProjectionService faceEmbeddingProjectionService;
    private final AiFaceVerificationClient aiFaceVerificationClient;

    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        Registrations registration = resolveCurrentStudentRegistration(request.getActivityId());
        validateActivityQrToken(registration.getActivity(), request.getVerifyCode());

        Optional<Attendances> existing = attendanceRepository.findByRegistrationId(registration.getId());
        if (existing.isPresent() && existing.get().getCheckinTime() != null) {
            return attendanceMapper.toResponse(existing.get(), "Ban da check-in hoat dong nay truoc do.");
        }

        Attendances attendance = existing.orElseGet(Attendances::new);
        attendance.setRegistration(registration);
        attendance.setCheckinTime(LocalDateTime.now());
        attendance.setMethod(request.getMethod() != null ? request.getMethod() : 1);
        attendance.setLatitude(request.getLatitude());
        attendance.setLongitude(request.getLongitude());
        attendance = attendanceRepository.save(attendance);

        return attendanceMapper.toResponse(attendance, "Check-in thanh cong. Vui long check-out sau khi hoan thanh hoat dong.");
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(CheckInRequest request) {
        Registrations registration = resolveCurrentStudentRegistration(request.getActivityId());
        validateActivityQrToken(registration.getActivity(), request.getVerifyCode());

        Attendances attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Ban phai check-in truoc khi check-out."));

        if (attendance.getCheckinTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ban phai check-in truoc khi check-out.");
        }

        if (attendance.getCheckoutTime() != null) {
            return attendanceMapper.toResponse(attendance, "Ban da check-out hoat dong nay truoc do.");
        }

        attendance.setCheckoutTime(LocalDateTime.now());
        if (request.getLatitude() != null) {
            attendance.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            attendance.setLongitude(request.getLongitude());
        }
        attendance = attendanceRepository.save(attendance);

        return attendanceMapper.toResponse(attendance, "Check-out thanh cong. Ban co the xac minh khuon mat de mo nop minh chung.");
    }

    @Override
    @Transactional
    public FaceCheckInResponse faceCheckIn(FaceCheckInRequest request, MultipartFile liveImage) {
        Registrations registration = resolveCurrentStudentRegistration(request.getActivityId());
        Attendances attendance = resolveAttendanceReadyForFaceVerification(registration);
        if (Integer.valueOf(1).equals(registration.getStatus())) {
            return alreadyCheckedInResponse(attendance);
        }

        Long studentId = registration.getStudent() != null ? registration.getStudent().getId() : null;
        StudentFaceEmbeddingProjection reference = faceEmbeddingProjectionService.getActive(studentId);

        long existingAttempts = faceCheckInAttemptRepository.countByRegistrationId(registration.getId());
        if (existingAttempts >= FACE_CHECK_IN_MAX_ATTEMPTS) {
            return maxAttemptsExceededResponse((int) existingAttempts);
        }

        int attemptNo = (int) existingAttempts + 1;
        FaceVerificationResult verification = verifyFace(reference, liveImage, attemptNo);
        verification = normalizeVerification(verification, attemptNo);
        saveFaceCheckInAttempt(registration, reference, verification);

        if (!Boolean.TRUE.equals(verification.getVerified())) {
            return toFaceCheckInResponse(verification, null);
        }

        faceEmbeddingProjectionService.markVerified(studentId, LocalDateTime.now());
        registration.setStatus(1);
        registrationRepository.save(registration);
        registrationKafkaProducer.sendCheckInSuccess(
                registration.getStudent().getId(),
                registration.getActivity().getId(),
                registration.getActivity().getTitle(),
                "Xac thuc khuon mat"
        );

        AttendanceResponse attendanceResponse = attendanceMapper.toResponse(
                attendance,
                "Xac thuc khuon mat thanh cong. Ban co the nop minh chung tham gia.");
        return toFaceCheckInResponse(verification, attendanceResponse);
    }

    private FaceVerificationResult verifyFace(
            StudentFaceEmbeddingProjection reference,
            MultipartFile liveImage,
            int attemptNo) {
        return aiFaceVerificationClient.verify(
                reference,
                readLiveImageBytes(liveImage),
                resolveLiveImageFilename(liveImage),
                attemptNo,
                FACE_CHECK_IN_MAX_ATTEMPTS);
    }

    private byte[] readLiveImageBytes(MultipartFile liveImage) {
        validateLiveImage(liveImage);
        try {
            return liveImage.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Khong doc duoc anh khuon mat de xac thuc");
        }
    }

    private void validateLiveImage(MultipartFile liveImage) {
        if (liveImage == null || liveImage.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Thieu anh khuon mat de xac thuc");
        }
        if (liveImage.getSize() > MAX_LIVE_IMAGE_BYTES) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Anh xac thuc khong duoc vuot qua 8MB");
        }
        String contentType = liveImage.getContentType();
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_ACTION, "File xac thuc phai la anh");
        }
    }

    private String resolveLiveImageFilename(MultipartFile liveImage) {
        String filename = liveImage.getOriginalFilename();
        return filename != null && !filename.isBlank() ? filename : "attendance-face-live.jpg";
    }

    private FaceVerificationResult normalizeVerification(FaceVerificationResult raw, int attemptNo) {
        boolean verified = Boolean.TRUE.equals(raw.getVerified());
        boolean allowRetry = !verified
                && attemptNo < FACE_CHECK_IN_MAX_ATTEMPTS
                && Boolean.TRUE.equals(raw.getAllowRetry());

        return FaceVerificationResult.builder()
                .verified(verified)
                .decision(raw.getDecision() != null
                        ? raw.getDecision()
                        : (verified ? "matched" : "not_matched"))
                .allowRetry(allowRetry)
                .attempt(attemptNo)
                .maxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS)
                .remainingAttempts(Math.max(FACE_CHECK_IN_MAX_ATTEMPTS - attemptNo, 0))
                .reasonCode(raw.getReasonCode())
                .message(resolveVerificationMessage(raw, verified, attemptNo))
                .threshold(raw.getThreshold())
                .distance(raw.getDistance())
                .similarity(raw.getSimilarity())
                .build();
    }

    private String resolveVerificationMessage(FaceVerificationResult raw, boolean verified, int attemptNo) {
        if (verified) {
            return raw.getMessage() != null
                    ? raw.getMessage()
                    : "Xac thuc khuon mat thanh cong. Ban co the nop minh chung tham gia.";
        }
        if (attemptNo >= FACE_CHECK_IN_MAX_ATTEMPTS) {
            return "Xac thuc khuon mat khong thanh cong. Ban da het so lan thu, vui long gui khieu nai neu can ho tro.";
        }
        return raw.getMessage() != null
                ? raw.getMessage()
                : "Xac thuc khuon mat khong thanh cong. Vui long chup lai anh.";
    }

    private void saveFaceCheckInAttempt(
            Registrations registration,
            StudentFaceEmbeddingProjection reference,
            FaceVerificationResult verification) {
        FaceCheckInAttempt attempt = new FaceCheckInAttempt();
        attempt.setRegistration(registration);
        attempt.setAttemptNo(verification.getAttempt());
        attempt.setMaxAttempts(verification.getMaxAttempts());
        attempt.setVerified(Boolean.TRUE.equals(verification.getVerified()));
        attempt.setAllowRetry(Boolean.TRUE.equals(verification.getAllowRetry()));
        attempt.setDecision(verification.getDecision());
        attempt.setReasonCode(verification.getReasonCode());
        attempt.setMessage(verification.getMessage());
        attempt.setThreshold(verification.getThreshold());
        attempt.setDistance(verification.getDistance());
        attempt.setSimilarity(verification.getSimilarity());
        attempt.setReferenceEmbeddingVersion(reference.getEmbeddingVersion());
        attempt.setReferenceModelName(reference.getModelName());
        faceCheckInAttemptRepository.save(attempt);
    }

    private FaceCheckInResponse alreadyCheckedInResponse(Attendances attendance) {
        AttendanceResponse attendanceResponse = attendanceMapper.toResponse(
                attendance,
                "Ban da xac minh khuon mat thanh cong truoc do!");
        return toFaceCheckInResponse(FaceVerificationResult.builder()
                .verified(true)
                .decision("already_face_verified")
                .allowRetry(false)
                .attempt(0)
                .maxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS)
                .remainingAttempts(0)
                .message("Ban da xac minh khuon mat thanh cong truoc do!")
                .build(), attendanceResponse);
    }

    private FaceCheckInResponse maxAttemptsExceededResponse(int existingAttempts) {
        return toFaceCheckInResponse(FaceVerificationResult.builder()
                .verified(false)
                .decision("max_attempts_exceeded")
                .allowRetry(false)
                .attempt(Math.min(existingAttempts, FACE_CHECK_IN_MAX_ATTEMPTS))
                .maxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS)
                .remainingAttempts(0)
                .reasonCode("MAX_ATTEMPTS_EXCEEDED")
                .message("Ban da het 5 lan xac thuc khuon mat. Vui long gui khieu nai neu can ho tro quyen loi.")
                .build(), null);
    }

    private Attendances resolveAttendanceReadyForFaceVerification(Registrations registration) {
        Attendances attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                        "Ban phai check-in va check-out truoc khi xac minh khuon mat."));

        if (attendance.getCheckinTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Ban phai check-in truoc khi xac minh khuon mat.");
        }

        if (attendance.getCheckoutTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Ban phai check-out truoc khi xac minh khuon mat.");
        }

        return attendance;
    }

    private Registrations resolveCurrentStudentRegistration(Long activityId) {
        Users student = getCurrentStudent();
        Registrations registration = registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Ban chua dang ky hoat dong nay nen khong the diem danh!"));

        if (registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ban da huy dang ky hoat dong nay roi!");
        }

        return registration;
    }

    private void validateActivityQrToken(Activities activity, String inputCode) {
        String dbQrToken = activity != null ? activity.getQrCodeToken() : null;
        if (dbQrToken == null || inputCode == null || !dbQrToken.equals(inputCode.trim())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ma QR diem danh khong hop le hoac khong thuoc hoat dong nay.");
        }
    }

    private FaceCheckInResponse toFaceCheckInResponse(FaceVerificationResult verification, AttendanceResponse attendance) {
        return FaceCheckInResponse.builder()
                .verified(verification.getVerified())
                .decision(verification.getDecision())
                .allowRetry(verification.getAllowRetry())
                .attempt(verification.getAttempt())
                .maxAttempts(verification.getMaxAttempts())
                .remainingAttempts(verification.getRemainingAttempts())
                .reasonCode(verification.getReasonCode())
                .message(verification.getMessage())
                .threshold(verification.getThreshold())
                .distance(verification.getDistance())
                .similarity(verification.getSimilarity())
                .attendance(attendance)
                .build();
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
    public void exportAttendanceToExcel(Long activityId, Long sessionId, OutputStream outputStream) throws Exception {
        List<Registrations> registrations = registrationRepository.findAllByActivityId(activityId);
        List<Attendances> attendances = attendanceRepository.findByRegistrationIn(registrations);

        String[] headers = {"STT", "MSSV", "Ho Ten", "Lop", "Gio Diem Danh", "Trang Thai"};
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
                            attendance.getCheckinTime() != null ? "Da check-in" : "Chua check-in"
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
