package com.example.activityservice.feature.attendances.service.Impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.impl.ActivityAccessSupport;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private static final int FACE_CHECK_IN_MAX_ATTEMPTS = 5;
    private static final long MAX_LIVE_IMAGE_BYTES = 8L * 1024L * 1024L;

    private final AttendanceRepository attendanceRepository;
    private final FaceCheckInAttemptRepository faceCheckInAttemptRepository;
    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;
    private final ExcelExportService excelExportService;
    private final RegistrationKafkaProducer registrationKafkaProducer;
    private final StudentFaceEmbeddingProjectionService faceEmbeddingProjectionService;
    private final AiFaceVerificationClient aiFaceVerificationClient;
    private final ActivityAccessSupport accessSupport;

    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        Registrations registration = resolveCurrentStudentRegistration(request.getActivityId());
        ActivitySchedule schedule = resolveRegisteredSchedule(registration, request.getScheduleId());
        validateAttendanceQrToken(registration.getActivity(), schedule, request.getVerifyCode());
        validateCheckInWindow(schedule);

        Optional<Attendances> existing = findAttendance(registration, schedule);
        if (existing.isPresent() && existing.get().getCheckinTime() != null) {
            return attendanceMapper.toResponse(existing.get(), "Bạn đã check-in buổi này trước đó.");
        }
        if (existing.isPresent() && Integer.valueOf(Attendances.STATUS_ABSENT).equals(existing.get().getStatus())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Buổi này đã kết thúc và được ghi nhận vắng mặt.");
        }

        Attendances attendance = existing.orElseGet(Attendances::new);
        attendance.setRegistration(registration);
        attendance.setSchedule(schedule);
        attendance.setCheckinTime(LocalDateTime.now());
        attendance.setMethod(request.getMethod() != null ? request.getMethod() : 1);
        attendance.setLatitude(request.getLatitude());
        attendance.setLongitude(request.getLongitude());
        attendance.setStatus(Attendances.STATUS_CHECKED_IN);
        attendance = attendanceRepository.save(attendance);

        return attendanceMapper.toResponse(attendance,
                "Check-in thành công. Vui lòng check-out sau khi hoàn thành buổi này.");
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(CheckInRequest request) {
        Registrations registration = resolveCurrentStudentRegistration(request.getActivityId());
        ActivitySchedule schedule = resolveRegisteredSchedule(registration, request.getScheduleId());
        validateAttendanceQrToken(registration.getActivity(), schedule, request.getVerifyCode());

        Attendances attendance = findAttendance(registration, schedule)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                        "Bạn phải check-in buổi này trước khi check-out."));

        if (attendance.getCheckinTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn phải check-in buổi này trước khi check-out.");
        }

        if (attendance.getCheckoutTime() != null) {
            return attendanceMapper.toResponse(attendance, "Bạn đã check-out buổi này trước đó.");
        }

        attendance.setCheckoutTime(LocalDateTime.now());
        if (request.getLatitude() != null) {
            attendance.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            attendance.setLongitude(request.getLongitude());
        }
        attendance.setStatus(Attendances.STATUS_CHECKED_OUT);
        attendance = attendanceRepository.save(attendance);

        return attendanceMapper.toResponse(attendance,
                "Check-out thành công. Bạn có thể xác minh khuôn mặt để mở nộp minh chứng.");
    }

    @Override
    @Transactional
    public FaceCheckInResponse faceCheckIn(FaceCheckInRequest request, MultipartFile liveImage) {
        Registrations registration = resolveCurrentStudentRegistration(request.getActivityId());
        ActivitySchedule schedule = resolveRegisteredSchedule(registration, request.getScheduleId());
        Attendances attendance = resolveAttendanceReadyForFaceVerification(registration, schedule);
        if (Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus())) {
            return alreadyCheckedInResponse(attendance);
        }

        Long studentId = registration.getStudent() != null ? registration.getStudent().getId() : null;
        StudentFaceEmbeddingProjection reference = faceEmbeddingProjectionService.getActive(studentId);

        long existingAttempts = countFaceAttempts(registration, schedule);
        if (existingAttempts >= FACE_CHECK_IN_MAX_ATTEMPTS) {
            Optional<FaceCheckInAttempt> latestAttempt = findLatestFaceAttempt(registration, schedule);
            if (latestAttempt.map(this::isSuccessfulFaceAttempt).orElse(false)) {
                FaceVerificationResult verification = toSuccessfulVerification(latestAttempt.get(), existingAttempts);
                AttendanceResponse attendanceResponse = completeFaceVerification(
                        studentId,
                        registration,
                        attendance,
                        request.getLatitude(),
                        request.getLongitude());
                return toFaceCheckInResponse(verification, attendanceResponse);
            }
            if (!latestAttempt.map(attempt -> Boolean.TRUE.equals(attempt.getAllowRetry())).orElse(false)) {
                return maxAttemptsExceededResponse((int) existingAttempts);
            }
        }

        int attemptNo = (int) existingAttempts + 1;
        FaceVerificationResult verification = verifyFace(reference, liveImage, attemptNo);
        verification = normalizeVerification(verification, attemptNo);
        saveFaceCheckInAttempt(registration, schedule, reference, verification);

        if (!Boolean.TRUE.equals(verification.getVerified())) {
            return toFaceCheckInResponse(verification, null);
        }

        faceEmbeddingProjectionService.markVerified(studentId, LocalDateTime.now());
        attendance.setStatus(Attendances.STATUS_FACE_VERIFIED);
        if (request.getLatitude() != null) {
            attendance.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            attendance.setLongitude(request.getLongitude());
        }
        attendance = attendanceRepository.save(attendance);
        if (hasVerifiedAllRegisteredSessions(registration)) {
            registration.setStatus(1);
            registrationRepository.save(registration);
            registrationKafkaProducer.sendCheckInSuccess(
                    registration.getStudent().getId(),
                    registration.getActivity().getId(),
                    registration.getActivity().getTitle(),
                    "Xác thực khuôn mặt");
        }

        AttendanceResponse attendanceResponse = attendanceMapper.toResponse(
                attendance,
                hasVerifiedAllRegisteredSessions(registration)
                        ? "Xác thực khuôn mặt thành công. Bạn có thể nộp minh chứng tham gia."
                        : "Xác thực khuôn mặt thành công cho buổi này. Vui lòng hoàn tất các buổi còn lại.");
        return toFaceCheckInResponse(verification, attendanceResponse);
    }

    private AttendanceResponse completeFaceVerification(
            Long studentId,
            Registrations registration,
            Attendances attendance,
            BigDecimal latitude,
            BigDecimal longitude) {
        faceEmbeddingProjectionService.markVerified(studentId, LocalDateTime.now());
        attendance.setStatus(Attendances.STATUS_FACE_VERIFIED);
        if (latitude != null) {
            attendance.setLatitude(latitude);
        }
        if (longitude != null) {
            attendance.setLongitude(longitude);
        }
        attendance = attendanceRepository.save(attendance);
        if (hasVerifiedAllRegisteredSessions(registration)) {
            registration.setStatus(1);
            registrationRepository.save(registration);
            registrationKafkaProducer.sendCheckInSuccess(
                    registration.getStudent().getId(),
                    registration.getActivity().getId(),
                    registration.getActivity().getTitle(),
                    "Xác thực khuôn mặt");
        }

        return attendanceMapper.toResponse(
                attendance,
                hasVerifiedAllRegisteredSessions(registration)
                        ? "Xác thực khuôn mặt thành công. Bạn có thể nộp minh chứng tham gia."
                        : "Xác thực khuôn mặt thành công cho buổi này. Vui lòng hoàn tất các buổi còn lại.");
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
            throw new AppException(ErrorCode.INVALID_ACTION, "Không đọc được ảnh khuôn mặt để xác thực");
        }
    }

    private void validateLiveImage(MultipartFile liveImage) {
        if (liveImage == null || liveImage.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Thiếu ảnh khuôn mặt để xác thực");
        }
        if (liveImage.getSize() > MAX_LIVE_IMAGE_BYTES) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Ảnh xác thực không được vượt quá 8MB");
        }
        String contentType = liveImage.getContentType();
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_ACTION, "File xác thực phải là ảnh");
        }
    }

    private String resolveLiveImageFilename(MultipartFile liveImage) {
        String filename = liveImage.getOriginalFilename();
        return filename != null && !filename.isBlank() ? filename : "attendance-face-live.jpg";
    }

    private FaceVerificationResult normalizeVerification(FaceVerificationResult raw, int attemptNo) {
        boolean verified = Boolean.TRUE.equals(raw.getVerified()) || isDistanceWithinThreshold(raw);
        boolean exhausted = !verified && attemptNo >= FACE_CHECK_IN_MAX_ATTEMPTS;
        boolean allowRetry = !verified
                && attemptNo < FACE_CHECK_IN_MAX_ATTEMPTS
                && Boolean.TRUE.equals(raw.getAllowRetry());

        return FaceVerificationResult.builder()
                .verified(verified)
                .decision(resolveVerificationDecision(raw, verified, exhausted))
                .allowRetry(allowRetry)
                .attempt(attemptNo)
                .maxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS)
                .remainingAttempts(Math.max(FACE_CHECK_IN_MAX_ATTEMPTS - attemptNo, 0))
                .reasonCode(verified ? null : (exhausted ? "MAX_ATTEMPTS_EXCEEDED" : raw.getReasonCode()))
                .message(resolveVerificationMessage(raw, verified, attemptNo))
                .threshold(raw.getThreshold())
                .distance(raw.getDistance())
                .similarity(raw.getSimilarity())
                .build();
    }

    private String resolveVerificationDecision(FaceVerificationResult raw, boolean verified, boolean exhausted) {
        if (verified) {
            return "MATCH";
        }
        if (exhausted) {
            return "max_attempts_exceeded";
        }
        return raw.getDecision() != null ? raw.getDecision() : "RETRY";
    }

    private boolean isDistanceWithinThreshold(FaceVerificationResult raw) {
        BigDecimal distance = raw != null ? raw.getDistance() : null;
        BigDecimal threshold = raw != null ? raw.getThreshold() : null;
        return distance != null && threshold != null && distance.compareTo(threshold) <= 0;
    }

    private String resolveVerificationMessage(FaceVerificationResult raw, boolean verified, int attemptNo) {
        if (verified) {
            return raw.getMessage() != null
                    ? raw.getMessage()
                : "Xác thực khuôn mặt thành công. Bạn có thể nộp minh chứng tham gia.";
        }
        if (attemptNo >= FACE_CHECK_IN_MAX_ATTEMPTS) {
            return "Xác thực khuôn mặt không thành công. Bạn đã hết số lần thử, vui lòng gửi khiếu nại nếu cần hỗ trợ.";
        }
        return raw.getMessage() != null
                ? raw.getMessage()
                : "Xác thực khuôn mặt không thành công. Vui lòng chụp lại ảnh.";
    }

    private void saveFaceCheckInAttempt(
            Registrations registration,
            ActivitySchedule schedule,
            StudentFaceEmbeddingProjection reference,
            FaceVerificationResult verification) {
        FaceCheckInAttempt attempt = new FaceCheckInAttempt();
        attempt.setRegistration(registration);
        attempt.setSchedule(schedule);
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
                "Bạn đã xác minh khuôn mặt thành công trước đó!");
        return toFaceCheckInResponse(FaceVerificationResult.builder()
                .verified(true)
                .decision("already_face_verified")
                .allowRetry(false)
                .attempt(0)
                .maxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS)
                .remainingAttempts(0)
                .message("Bạn đã xác minh khuôn mặt thành công trước đó!")
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
                .message("Bạn đã hết 5 lần xác thực khuôn mặt. Vui lòng gửi khiếu nại nếu cần hỗ trợ quyền lợi.")
                .build(), null);
    }

    private Attendances resolveAttendanceReadyForFaceVerification(Registrations registration,
            ActivitySchedule schedule) {
        Attendances attendance = findAttendance(registration, schedule)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                        "Bạn phải check-in và check-out buổi này trước khi xác minh khuôn mặt."));

        if (attendance.getCheckinTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Bạn phải check-in buổi này trước khi xác minh khuôn mặt.");
        }

        if (attendance.getCheckoutTime() == null) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Bạn phải check-out buổi này trước khi xác minh khuôn mặt.");
        }

        return attendance;
    }

    private ActivitySchedule resolveRegisteredSchedule(Registrations registration, Long scheduleId) {
        List<ActivitySchedule> registeredSchedules = registration.getRegisteredSchedules() != null
                ? registration.getRegisteredSchedules()
                : List.of();

        if (registeredSchedules.isEmpty()) {
            if (scheduleId != null) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Bạn chưa đăng ký buổi này.");
            }
            return null;
        }

        if (scheduleId == null) {
            if (registeredSchedules.size() == 1) {
                return registeredSchedules.get(0);
            }
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn buổi cần điểm danh.");
        }

        return registeredSchedules.stream()
                .filter(schedule -> schedule != null && Objects.equals(schedule.getId(), scheduleId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Bạn chưa đăng ký buổi này."));
    }

    private Optional<Attendances> findAttendance(Registrations registration, ActivitySchedule schedule) {
        if (schedule == null) {
            return attendanceRepository.findFirstByRegistrationIdAndScheduleIsNullOrderByIdAsc(
                    registration.getId());
        }
        return attendanceRepository.findFirstByRegistrationIdAndScheduleIdOrderByIdAsc(
                registration.getId(), schedule.getId());
    }

    private void validateCheckInWindow(ActivitySchedule schedule) {
        if (schedule == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (schedule.getStartTime() != null && now.isBefore(schedule.getStartTime())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Buổi này chưa đến thời gian check-in.");
        }
        if (schedule.getEndTime() != null && now.isAfter(schedule.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Buổi này đã kết thúc, không thể check-in.");
        }
    }

    private long countFaceAttempts(Registrations registration, ActivitySchedule schedule) {
        if (schedule == null) {
            return faceCheckInAttemptRepository.countByRegistrationIdAndScheduleIsNull(registration.getId());
        }
        return faceCheckInAttemptRepository.countByRegistrationIdAndScheduleId(registration.getId(), schedule.getId());
    }

    private Optional<FaceCheckInAttempt> findLatestFaceAttempt(Registrations registration, ActivitySchedule schedule) {
        if (schedule == null) {
            return faceCheckInAttemptRepository.findTopByRegistrationIdAndScheduleIsNullOrderByAttemptNoDesc(
                    registration.getId());
        }
        return faceCheckInAttemptRepository.findTopByRegistrationIdAndScheduleIdOrderByAttemptNoDesc(
                registration.getId(),
                schedule.getId());
    }

    private boolean isSuccessfulFaceAttempt(FaceCheckInAttempt attempt) {
        if (attempt == null) {
            return false;
        }
        if (Boolean.TRUE.equals(attempt.getVerified())) {
            return true;
        }
        BigDecimal distance = attempt.getDistance();
        BigDecimal threshold = attempt.getThreshold();
        return distance != null && threshold != null && distance.compareTo(threshold) <= 0;
    }

    private FaceVerificationResult toSuccessfulVerification(FaceCheckInAttempt attempt, long existingAttempts) {
        return FaceVerificationResult.builder()
                .verified(true)
                .decision("MATCH")
                .allowRetry(false)
                .attempt((int) Math.min(existingAttempts, FACE_CHECK_IN_MAX_ATTEMPTS))
                .maxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS)
                .remainingAttempts(0)
                .reasonCode(null)
                .message("Xác thực khuôn mặt thành công. Bạn có thể nộp minh chứng tham gia.")
                .threshold(attempt.getThreshold())
                .distance(attempt.getDistance())
                .similarity(attempt.getSimilarity())
                .build();
    }

    private boolean hasVerifiedAllRegisteredSessions(Registrations registration) {
        List<ActivitySchedule> registeredSchedules = registration.getRegisteredSchedules() != null
                ? registration.getRegisteredSchedules()
                : List.of();
        List<Attendances> attendances = attendanceRepository.findAllByRegistrationId(registration.getId());

        if (registeredSchedules.isEmpty()) {
            return attendances.stream()
                    .anyMatch(attendance -> attendance.getSchedule() == null
                            && Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus())
                            && attendance.getCheckinTime() != null
                            && attendance.getCheckoutTime() != null);
        }

        Set<Long> verifiedScheduleIds = attendances.stream()
                .filter(attendance -> Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus()))
                .filter(attendance -> attendance.getCheckinTime() != null && attendance.getCheckoutTime() != null)
                .map(attendance -> attendance.getSchedule())
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .collect(Collectors.toSet());

        return registeredSchedules.stream()
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .allMatch(verifiedScheduleIds::contains);
    }

    private Registrations resolveCurrentStudentRegistration(Long activityId) {
        Users student = getCurrentStudent();
        Registrations registration = registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                        "Bạn chưa đăng ký hoạt động này nên không thể điểm danh!"));

        if (registration.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hủy đăng ký hoạt động này rồi!");
        }
        if (Integer.valueOf(Registrations.STATUS_ABSENT).equals(registration.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Đăng ký này đã được ghi nhận vắng mặt.");
        }

        return registration;
    }

    private void validateAttendanceQrToken(Activities activity, ActivitySchedule schedule, String inputCode) {
        if (schedule != null) {
            validateScheduleQrToken(schedule, inputCode);
            return;
        }
        validateActivityQrToken(activity, inputCode);
    }

    private void validateScheduleQrToken(ActivitySchedule schedule, String inputCode) {
        String dbQrToken = schedule != null ? schedule.getQrCodeToken() : null;
        if (dbQrToken == null || inputCode == null || !dbQrToken.equals(inputCode.trim())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Mã QR điểm danh không hợp lệ hoặc không thuộc buổi hoạt động này.");
        }
    }

    private void validateActivityQrToken(Activities activity, String inputCode) {
        String dbQrToken = activity != null ? activity.getQrCodeToken() : null;
        if (dbQrToken == null || inputCode == null || !dbQrToken.equals(inputCode.trim())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Mã QR điểm danh không hợp lệ hoặc không thuộc hoạt động này.");
        }
    }

    private FaceCheckInResponse toFaceCheckInResponse(FaceVerificationResult verification,
            AttendanceResponse attendance) {
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
    @Transactional
    public PageDTO<AttendanceResponse> getAttendancesBySession(Long activityId, Long sessionId, Pageable pageable) {
        ensureCanManageActivity(activityId);
        recordExpiredAbsences();
        List<Registrations> registrations = findRegistrationsForAttendanceScope(activityId, sessionId);

        List<Attendances> attendances = sessionId != null
                ? attendanceRepository.findByRegistrationInAndScheduleId(registrations, sessionId)
                : attendanceRepository.findByRegistrationIn(registrations);

        List<AttendanceResponse> responses = attendances.stream()
                .map(a -> attendanceMapper.toResponse(a, null))
                .collect(Collectors.toList());

        // Simple pagination simulation
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<AttendanceResponse> pageContent = start >= responses.size()
                ? List.of()
                : responses.subList(start, end);

        int totalPages = (int) Math.ceil((double) responses.size() / pageable.getPageSize());
        PageDTO<AttendanceResponse> result = new PageDTO<>();
        result.setPageNumber(pageable.getPageNumber() + 1);
        result.setPageSize(pageable.getPageSize());
        result.setTotalPage(totalPages);
        result.setTotalRows(responses.size());
        result.setData(pageContent);
        result.setLast(pageable.getPageNumber() + 1 >= totalPages);
        return result;
    }

    @Override
    @Transactional
    public void exportAttendanceToExcel(Long activityId, Long sessionId, OutputStream outputStream) throws Exception {
        ensureCanManageActivity(activityId);
        recordExpiredAbsences();
        List<Registrations> registrations = findRegistrationsForAttendanceScope(activityId, sessionId);
        List<Attendances> attendances = sessionId != null
                ? attendanceRepository.findByRegistrationInAndScheduleId(registrations, sessionId)
                : attendanceRepository.findByRegistrationIn(registrations);

        String[] headers = { "STT", "MSSV", "Họ tên", "Lớp", "Buổi", "Giờ Check-in", "Giờ Check-out", "Trạng thái" };
        java.util.concurrent.atomic.AtomicInteger stt = new java.util.concurrent.atomic.AtomicInteger(1);

        excelExportService.export(
                "Danh_sach_Diem_Danh",
                headers,
                attendances,
                (attendance) -> {
                    Registrations reg = attendance.getRegistration();
                    Users student = reg.getStudent();

                    return new Object[] {
                            stt.getAndIncrement(),
                            student.getStudentCode() != null ? student.getStudentCode() : "",
                            student.getFullName() != null ? student.getFullName() : student.getUsername(),
                            "", // Lop - can join with profile
                            attendance.getSchedule() != null ? attendance.getSchedule().getTitle() : "",
                            attendance.getCheckinTime(),
                            attendance.getCheckoutTime(),
                            attendanceMapper.toResponse(attendance, null).getAttendanceStatus()
                    };
                },
                outputStream);
    }

    @Override
    @Transactional
    public AttendanceStatisticsResponse getStatistics(Long activityId, Long sessionId) {
        ensureCanManageActivity(activityId);
        recordExpiredAbsences();
        List<Registrations> registrations = findRegistrationsForAttendanceScope(activityId, sessionId);
        List<Attendances> attendances = sessionId != null
                ? attendanceRepository.findByRegistrationInAndScheduleId(registrations, sessionId)
                : attendanceRepository.findByRegistrationIn(registrations);

        int totalRegistrations = expectedAttendanceSlots(registrations, sessionId);
        int totalAttendances = (int) attendances.stream()
                .filter(a -> Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(a.getStatus()))
                .count();
        int totalAbsences = (int) attendances.stream()
                .filter(a -> Integer.valueOf(Attendances.STATUS_ABSENT).equals(a.getStatus()))
                .count();
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
                .presentMale(0) // Placeholder
                .presentFemale(0)
                .absentMale(0)
                .absentFemale(0)
                .build();
    }

    private List<Registrations> findRegistrationsForAttendanceScope(Long activityId, Long sessionId) {
        List<Registrations> registrations = registrationRepository.findAllByActivityId(activityId).stream()
                .filter(registration -> registration.getStatus() == null || registration.getStatus() != 2)
                .collect(Collectors.toList());
        if (sessionId == null) {
            return registrations;
        }
        return registrations.stream()
                .filter(registration -> registration.getRegisteredSchedules() != null
                        && registration.getRegisteredSchedules().stream()
                                .anyMatch(schedule -> schedule != null && Objects.equals(schedule.getId(), sessionId)))
                .collect(Collectors.toList());
    }

    private int expectedAttendanceSlots(List<Registrations> registrations, Long sessionId) {
        if (sessionId != null) {
            return registrations.size();
        }
        return registrations.stream()
                .mapToInt(registration -> {
                    int sessionCount = registration.getRegisteredSchedules() != null
                            ? registration.getRegisteredSchedules().size()
                            : 0;
                    return sessionCount > 0 ? sessionCount : 1;
                })
                .sum();
    }

    @Scheduled(fixedDelayString = "${app.attendance.absence-scan-ms:60000}")
    @Transactional
    public void recordExpiredAbsences() {
        LocalDateTime now = LocalDateTime.now();
        attendanceRepository.markPendingAttendancesAbsent(
                now,
                Attendances.STATUS_PENDING,
                Attendances.STATUS_ABSENT);

        List<Object[]> missingRows = attendanceRepository.findMissingAttendanceRowsForEndedSchedules(now);
        List<Attendances> absences = new ArrayList<>();
        for (Object[] row : missingRows) {
            if (row.length < 2 || !(row[0] instanceof Registrations registration)
                    || !(row[1] instanceof ActivitySchedule schedule)) {
                continue;
            }
            Attendances absence = new Attendances();
            absence.setRegistration(registration);
            absence.setSchedule(schedule);
            absence.setStatus(Attendances.STATUS_ABSENT);
            absences.add(absence);
        }
        if (!absences.isEmpty()) {
            attendanceRepository.saveAll(absences);
        }

        markRegistrationsAbsent(now);
    }

    private void markRegistrationsAbsent(LocalDateTime now) {
        List<Registrations> candidates = registrationRepository.findRegistrationsForAbsenceScan();
        List<Registrations> absentRegistrations = new ArrayList<>();

        for (Registrations registration : candidates) {
            if (!isRegistrationFinished(registration, now) || hasVerifiedAllRegisteredSessions(registration)) {
                continue;
            }

            registration.setStatus(Registrations.STATUS_ABSENT);
            if (registration.getAbsenceReason() == null || registration.getAbsenceReason().isBlank()) {
                registration.setAbsenceReason(
                        "Sinh viên đã đăng ký nhưng không hoàn tất điểm danh trước khi hoạt động kết thúc.");
            }
            registration.setAbsenceReviewed(false);
            registration.setAbsenceReviewedBy(null);
            registration.setAbsenceReviewedAt(null);
            registration.setAbsenceReviewNote(null);
            absentRegistrations.add(registration);

            if (registration.getRegisteredSchedules() == null || registration.getRegisteredSchedules().isEmpty()) {
                attendanceRepository.findFirstByRegistrationIdAndScheduleIsNullOrderByIdAsc(registration.getId())
                        .orElseGet(() -> {
                            Attendances absence = new Attendances();
                            absence.setRegistration(registration);
                            absence.setStatus(Attendances.STATUS_ABSENT);
                            return attendanceRepository.save(absence);
                        });
            }
        }

        if (!absentRegistrations.isEmpty()) {
            registrationRepository.saveAll(absentRegistrations);
        }
    }

    private boolean isRegistrationFinished(Registrations registration, LocalDateTime now) {
        List<ActivitySchedule> schedules = registration.getRegisteredSchedules() != null
                ? registration.getRegisteredSchedules()
                : List.of();

        if (!schedules.isEmpty()) {
            return schedules.stream()
                    .allMatch(schedule -> schedule != null
                            && schedule.getEndTime() != null
                            && now.isAfter(schedule.getEndTime()));
        }

        return registration.getActivity() != null
                && registration.getActivity().getEndDate() != null
                && now.isAfter(registration.getActivity().getEndDate());
    }

    private void ensureCanManageActivity(Long activityId) {
        if (activityId == null) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Thiếu mã hoạt động.");
        }
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động!"));
        if (accessSupport.isCurrentAdmin()) {
            return;
        }
        if (accessSupport.isCurrentDepartment()) {
            accessSupport.ensureCurrentDepartmentCanManageActivity(activity);
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem điểm danh của hoạt động này.");
    }
}
