package com.example.activityservice.feature.registration.service.Impl;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.activities.service.ActivityCacheService;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.attendances.model.FaceCheckInAttempt;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
import com.example.activityservice.feature.attendances.repository.FaceCheckInAttemptRepository;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.activityservice.feature.face_embedding.service.StudentFaceEmbeddingProjectionService;
import com.example.activityservice.feature.proofs.repository.ProofRepository;
import com.example.activityservice.feature.registration.dto.RegistrationRequest;
import com.example.activityservice.feature.registration.dto.RegistrationResponse;
import com.example.activityservice.feature.registration.dto.AbsenceReviewRequest;
import com.example.activityservice.feature.registration.kafka.RegistrationKafkaProducer;
import com.example.activityservice.feature.registration.mapper.RegistrationMapper;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.activityservice.feature.registration.service.RegistrationService;
import com.example.activityservice.feature.activities.service.impl.ActivityAccessSupport;
import com.example.activityservice.service.ExcelExportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {
    private static final int FACE_CHECK_IN_MAX_ATTEMPTS = 5;
    private static final DateTimeFormatter SCHEDULE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ABSENCE_VIOLATION_PROCESSED_REFERENCE_TYPE = "activity-absence-violation-processed";
    private static final String ABSENCE_REVIEW_PROCESSED_REFERENCE_TYPE = "activity-absence-review-processed";

    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final ExcelExportService excelExportService;
    private final ProofRepository proofRepository;
    private final RegistrationKafkaProducer registrationKafkaProducer;
    private final ActivityCacheService activityCacheService;
    private final StudentFaceEmbeddingProjectionService faceEmbeddingProjectionService;
    private final FaceCheckInAttemptRepository faceCheckInAttemptRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final ActivityAccessSupport accessSupport;

    // --- Lấy sinh viên đang đăng nhập ---
    public Users getCurrentStudent() {
        Jwt jwt = (Jwt) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        assert jwt != null;
        String username = jwt.getClaimAsString("preferred_username");

        // ĐÃ SỬA: Không gọi IdentityService nữa. Nếu chưa có thì ném lỗi bắt chờ Kafka
        // đồng bộ
        return userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("User {} chưa được Kafka đồng bộ xuống Activity DB.", username);
            return new AppException(ErrorCode.USER_NOT_EXISTED,
                    "Dữ liệu tài khoản đang được đồng bộ, vui lòng thử lại sau ít phút!");
        });
    }

    // ĐÃ SỬA: Lấy dữ liệu Profile thẳng từ bảng Users Local, không gọi
    // ProfileClient
    private Users getCurrentStudentForRegistration() {
        Users currentStudent = getCurrentStudent();
        return userRepository.findByIdForRegistrationUpdate(currentStudent.getId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_EXISTED,
                        "Không tìm thấy dữ liệu sinh viên để thực hiện đăng ký."));
    }

    private void populateProfileData(List<RegistrationResponse> responses) {
        if (responses == null || responses.isEmpty())
            return;

        List<Long> userIds = responses.stream()
                .filter(Objects::nonNull)
                .map(res -> res.getStudentId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Users> usersList = userRepository.findAllById(userIds);
        Map<Long, Users> userMap = usersList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(u -> u.getId(), u -> u));

        for (RegistrationResponse res : responses) {
            Users u = userMap.get(res.getStudentId());
            if (u != null) {
                res.setStudentName(u.getFullName() != null ? u.getFullName() : u.getUsername());
                res.setStudentCode(u.getStudentCode());
                res.setAvatarUrl(u.getAvatarUrl());
                res.setClassId(u.getClassId());
                res.setClassCode(u.getClassCode());
                res.setClassName(u.getClassName());
                res.setAcademicYear(u.getAcademicYear());
            }
        }
    }

    // --- Xử lý bộ lọc tìm kiếm ---
    private Specification<Registrations> buildFilterSpecification(
            Long activityId,
            String status,
            String academicYear,
            List<Long> searchedUserIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("activity").get("id"), activityId));

            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                try {
                    int statusCode = Integer.parseInt(status);
                    predicates.add(cb.equal(root.get("status"), statusCode));
                } catch (NumberFormatException ignored) {
                }
            }

            if (StringUtils.hasText(academicYear)) {
                predicates.add(cb.equal(root.get("student").get("academicYear"), academicYear.trim()));
            }

            if (searchedUserIds != null) {
                if (searchedUserIds.isEmpty()) {
                    predicates.add(cb.disjunction()); // Không tìm thấy user nào khớp keyword -> Trả về rỗng
                } else {
                    predicates.add(root.get("student").get("id").in(searchedUserIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationResponse getMyStatusByActivity(Long activityId) {
        Users student = getCurrentStudent();
        return registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .map(registration -> registrationMapper.toResponseWithProof(
                        registration, resolveProofStatus(registration)))
                .orElse(null);
    }

    @Override
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        Users student = getCurrentStudentForRegistration();

        Activities activity = activityRepository.findByIdForRegistrationUpdate(request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Hoạt động không tồn tại"));

        if (activity.getStatus() != 1)
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này hiện chưa mở đăng ký.");
        validateStudentEligibleForActivity(student, activity);

        LocalDateTime now = LocalDateTime.now();
        if (activity.getRegistrationStart() == null
                || activity.getRegistrationEnd() == null
                || !activity.getRegistrationStart().isBefore(activity.getRegistrationEnd())
                || now.isBefore(activity.getRegistrationStart())
                || now.isAfter(activity.getRegistrationEnd()))
            throw new AppException(ErrorCode.INVALID_ACTION, "Rất tiếc, đã hết hoặc chưa tới thời hạn đăng ký.");

        Registrations existingReg = registrationRepository
                .findByStudentIdAndActivityId(student.getId(), activity.getId()).orElse(null);
        if (existingReg != null && (existingReg.getStatus() == null
                || existingReg.getStatus() == Registrations.STATUS_REGISTERED
                || existingReg.getStatus() == Registrations.STATUS_ATTENDED
                || existingReg.getStatus() == Registrations.STATUS_ABSENT))
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã đăng ký hoạt động này rồi nha!");
        validateActivityHasAvailableSlot(activity);

        List<ActivitySchedule> selectedSchedules = resolveSelectedSchedules(activity, request.getScheduleIds());
        validateNoScheduleConflicts(student.getId(), activity, selectedSchedules);
        faceEmbeddingProjectionService.ensureActiveForRegistration(student.getId());

        Registrations regToSave;
        if (existingReg != null && existingReg.getStatus() == 2) {
            regToSave = existingReg;
            registrationMapper.reRegisterEntity(regToSave, selectedSchedules);
        } else {
            regToSave = registrationMapper.toNewEntity(student, activity, selectedSchedules);
        }

        Registrations saved = registrationRepository.save(regToSave);
        syncPendingAttendances(saved, selectedSchedules);

        // Gui Kafka notification
        registrationKafkaProducer.sendRegistrationSuccess(
                student.getId(),
                activity.getId(),
                activity.getTitle(),
                null);

        activityCacheService.evictActivityListCaches();
        return registrationMapper.toResponse(saved);
    }

    private void validateStudentEligibleForActivity(Users student, Activities activity) {
        if (!isFacultyInternalActivity(activity)) {
            return;
        }
        if (student.getDepartmentId() == null
                || activity.getDepartmentId() == null
                || !Objects.equals(student.getDepartmentId(), activity.getDepartmentId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không thuộc khoa được phép đăng ký hoạt động này.");
        }
    }

    private boolean isFacultyInternalActivity(Activities activity) {
        return Boolean.TRUE.equals(activity.getIsFaculty()) && !Boolean.TRUE.equals(activity.getIsExternal());
    }

    private void validateActivityHasAvailableSlot(Activities activity) {
        if (registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2) >= activity
                .getMaxParticipants()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này đã full chỗ mất rồi!");
        }
    }

    @Override
    @Transactional
    public RegistrationResponse cancelByActivityId(Long activityId, String reason) {
        Users student = getCurrentStudentForRegistration();
        Registrations reg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                        "Bạn chưa đăng ký hoạt động này nên không hủy được!"));
        return this.processCancellation(reg, reason);
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long regId, String reason) {
        Users student = getCurrentStudentForRegistration();
        Registrations reg = registrationRepository.findById(regId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED));
        if (reg.getStudent() == null || !Objects.equals(reg.getStudent().getId(), student.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền hủy đăng ký này.");
        }
        return this.processCancellation(reg, reason);
    }

    private RegistrationResponse processCancellation(Registrations reg, String reason) {
        if (reg.getStatus() == 1)
            throw new AppException(ErrorCode.INVALID_ACTION, "Không được hủy khi đã điểm danh!");
        if (reg.getStatus() == 2)
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hủy đăng ký trước đó rồi!");

        validateCancellationWindow(reg.getActivity(), LocalDateTime.now());

        Long studentId = reg.getStudent().getId();
        Long activityId = reg.getActivity().getId();
        String activityTitle = reg.getActivity().getTitle();

        registrationMapper.cancelEntity(reg, reason);

        Registrations saved = registrationRepository.save(reg);

        // Gui Kafka notification
        registrationKafkaProducer.sendCancellationSuccess(studentId, activityId, activityTitle, reason);

        activityCacheService.evictActivityListCaches();
        return registrationMapper.toResponse(saved);
    }

    private void validateCancellationWindow(Activities activity, LocalDateTime now) {
        if (activity == null || activity.getStartDate() == null) {
            throw new AppException(
                    ErrorCode.INVALID_ACTION,
                    "Không thể xác định thời gian bắt đầu hoạt động để hủy đăng ký.");
        }

        boolean activityStarted = !now.isBefore(activity.getStartDate());
        boolean activityEnded = activity.getEndDate() != null && !now.isBefore(activity.getEndDate());
        if (activityStarted || activityEnded) {
            throw new AppException(
                    ErrorCode.INVALID_ACTION,
                    "Không được hủy đăng ký khi hoạt động đã bắt đầu hoặc kết thúc.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<RegistrationResponse> getParticipants(
            Long activityId,
            String keyword,
            String status,
            String academicYear,
            Pageable pageable) {
        List<Long> searchedUserIds = null;

        // ĐÃ SỬA: Tìm user bằng UserRepository Local
        if (StringUtils.hasText(keyword)) {
            searchedUserIds = userRepository.searchIdsByKeyword(keyword);
        }

        Specification<Registrations> spec = buildFilterSpecification(activityId, status, academicYear, searchedUserIds);
        Page<Registrations> pageData = registrationRepository.findAll(spec, pageable);

        List<RegistrationResponse> dtoList = pageData.getContent().stream()
                .map(registrationMapper::toResponse)
                .collect(Collectors.toList());

        populateProfileData(dtoList);

        PageDTO<RegistrationResponse> result = new PageDTO<>();
        result.setPageNumber(pageable.getPageNumber() + 1);
        result.setPageSize(pageable.getPageSize());
        result.setTotalPage(pageData.getTotalPages());
        result.setTotalRows(pageData.getTotalElements());
        result.setData(dtoList);
        result.setLast(pageData.isLast());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getParticipantAcademicYears(Long activityId) {
        return registrationRepository.findDistinctAcademicYearsByActivityId(activityId);
    }

    @Override
    @Transactional
    public RegistrationResponse updateStatus(Long id, Integer status) {
        return updateStatus(id, status, false);
    }

    @Override
    @Transactional
    public RegistrationResponse updateStatus(Long id, Integer status, boolean processViolation) {
        Registrations reg = registrationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy đơn đăng ký!"));
        LocalDateTime now = LocalDateTime.now();
        boolean absenceViolationProcessed = processViolation
                && Integer.valueOf(Registrations.STATUS_CANCELLED).equals(status)
                && Integer.valueOf(Registrations.STATUS_REGISTERED).equals(reg.getStatus())
                && isRegistrationFinished(reg, now);
        LocalDateTime processedAt = absenceViolationProcessed ? now : null;

        if (Integer.valueOf(Registrations.STATUS_ABSENT).equals(status)) {
            reg.setStatus(Registrations.STATUS_ABSENT);
            reg.setAbsenceReason("Được ghi nhận vắng mặt bởi BTC.");
            reg.setAbsenceReviewed(false);
            reg.setAbsenceReviewedBy(null);
            reg.setAbsenceReviewedAt(null);
            reg.setAbsenceReviewNote(null);
        } else if (status == 2) {
            registrationMapper.cancelEntity(reg, "Quản trị viên / Khoa hủy đăng ký");
        } else {
            if (Integer.valueOf(2).equals(reg.getStatus())) {
                Activities activity = activityRepository.findByIdForRegistrationUpdate(reg.getActivity().getId())
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                "Hoạt động không tồn tại"));
                validateActivityHasAvailableSlot(activity);
            }
            reg.setStatus(status);
        }

        Registrations saved = registrationRepository.save(reg);
        if (absenceViolationProcessed) {
            notifyAbsenceViolationProcessed(
                    saved,
                    ABSENCE_VIOLATION_PROCESSED_REFERENCE_TYPE,
                    "Vi phạm vắng điểm danh đã được xử lý",
                    "BTC đã xử lý vi phạm vắng điểm danh của bạn trong hoạt động '"
                            + activityTitle(saved) + "'. Đăng ký của bạn đã bị hủy.",
                    processedAt);
        }

        RegistrationResponse response = registrationMapper.toResponse(saved);
        activityCacheService.evictActivityListCaches();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public void exportToExcel(
            Long activityId,
            String keyword,
            String status,
            String academicYear,
            OutputStream outputStream) {
        List<Long> searchedUserIds = null;

        // ĐÃ SỬA: Tìm user bằng UserRepository Local
        if (StringUtils.hasText(keyword)) {
            searchedUserIds = userRepository.searchIdsByKeyword(keyword);
        }

        Specification<Registrations> spec = buildFilterSpecification(activityId, status, academicYear, searchedUserIds);
        List<Registrations> list = registrationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "registeredAt"));

        List<RegistrationResponse> dtoList = list.stream().map(registrationMapper::toResponse)
                .collect(Collectors.toList());
        populateProfileData(dtoList);

        String[] headers = { "STT", "MSSV", "Họ và Tên", "Thời gian ĐK", "Buổi đăng ký", "Trạng thái", "Lý do hủy" };
        java.util.concurrent.atomic.AtomicInteger stt = new java.util.concurrent.atomic.AtomicInteger(1);
        try {
            excelExportService.export(
                    "Danh_sach_SV",
                    headers,
                    dtoList,
                    (dto) -> {
                        String statusStr = dto.getStatus() == 0 ? "Đã đăng ký"
                                : (dto.getStatus() == 1 ? "Đã tham gia" : "Đã hủy");

                        String schedulesStr = "";
                        if (dto.getScheduleIds() != null && !dto.getScheduleIds().isEmpty()) {
                            schedulesStr = dto.getScheduleIds().size() + " buổi đăng ký";
                        }

                        return new Object[] {
                                stt.getAndIncrement(),
                                dto.getStudentCode() != null ? dto.getStudentCode() : "",
                                dto.getStudentName() != null ? dto.getStudentName() : "",
                                dto.getRegisteredAt(),
                                schedulesStr,
                                statusStr,
                                dto.getCancelReason() != null ? dto.getCancelReason() : ""
                        };
                    },
                    outputStream);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Lỗi khi xuất file Excel!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistrationResponse> getMyRecords(Long semesterId) {
        Users student = getCurrentStudent();

        Specification<Registrations> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("student").get("id"), student.getId()));
            if (semesterId != null) {
                predicates.add(cb.equal(root.get("activity").get("semester").get("id"), semesterId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Registrations> myRecords = registrationRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "registeredAt"));

        List<RegistrationResponse> responseList = myRecords.stream().map(reg -> {
            int proofStatus = resolveProofStatus(reg);
            RegistrationResponse response = registrationMapper.toResponseWithProof(reg, proofStatus);
            enrichFaceVerificationState(response, reg);
            return response;
        }).collect(Collectors.toList());

        populateProfileData(responseList);

        return responseList;
    }

    private int resolveProofStatus(Registrations registration) {
        return proofRepository.findFirstByRegistrationIdOrderByCreatedAtDescIdDesc(registration.getId())
                .map(proof -> proof.getStatus())
                .map(status -> status == 0 ? 1 : status == 1 ? 2 : status == 2 ? 3 : 0)
                .orElse(0);
    }

    @Override
    @Transactional
    public PageDTO<RegistrationResponse> getAbsentParticipants(
            Long activityId,
            String keyword,
            String academicYear,
            Boolean reviewed,
            Pageable pageable) {
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động."));
        if (!accessSupport.isCurrentAdmin()) {
            accessSupport.ensureCurrentDepartmentCanManageActivity(activity);
        }
        attendanceService.recordExpiredAbsences();

        List<Long> searchedUserIds = null;
        if (StringUtils.hasText(keyword)) {
            searchedUserIds = userRepository.searchIdsByKeyword(keyword);
        }

        Specification<Registrations> spec = buildFilterSpecification(
                activityId,
                String.valueOf(Registrations.STATUS_ABSENT),
                academicYear,
                searchedUserIds);
        if (reviewed != null) {
            Boolean reviewedFilter = reviewed;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("absenceReviewed"), reviewedFilter));
        }

        Page<Registrations> pageData = registrationRepository.findAll(spec, pageable);
        List<RegistrationResponse> dtoList = pageData.getContent().stream()
                .map(registration -> registrationMapper.toResponseWithProof(
                        registration, resolveProofStatus(registration)))
                .collect(Collectors.toList());
        populateProfileData(dtoList);

        PageDTO<RegistrationResponse> result = new PageDTO<>();
        result.setPageNumber(pageable.getPageNumber() + 1);
        result.setPageSize(pageable.getPageSize());
        result.setTotalPage(pageData.getTotalPages());
        result.setTotalRows(pageData.getTotalElements());
        result.setData(dtoList);
        result.setLast(pageData.isLast());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageDTO<RegistrationResponse> getStudentsWithoutProof(Long activityId, Pageable pageable) {
        Activities activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy hoạt động."));
        if (!accessSupport.isCurrentAdmin()) {
            accessSupport.ensureCurrentDepartmentCanManageActivity(activity);
        }
        if (activity.getEndDate() == null || !LocalDateTime.now().isAfter(activity.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Chỉ có thể lấy danh sách sinh viên chưa nộp minh chứng sau khi hoạt động kết thúc.");
        }

        Page<Registrations> pageData = registrationRepository.findEligibleRegistrationsWithoutProof(activityId,
                pageable);
        List<RegistrationResponse> dtoList = pageData.getContent().stream()
                .map(registration -> registrationMapper.toResponseWithProof(registration, 0))
                .collect(Collectors.toList());
        populateProfileData(dtoList);

        PageDTO<RegistrationResponse> result = new PageDTO<>();
        result.setPageNumber(pageable.getPageNumber() + 1);
        result.setPageSize(pageable.getPageSize());
        result.setTotalPage(pageData.getTotalPages());
        result.setTotalRows(pageData.getTotalElements());
        result.setData(dtoList);
        result.setLast(pageData.isLast());
        return result;
    }

    @Override
    @Transactional
    public RegistrationResponse reviewAbsence(Long registrationId, AbsenceReviewRequest request) {
        Registrations registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy đăng ký."));

        if (!Integer.valueOf(Registrations.STATUS_ABSENT).equals(registration.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Sinh viên này không ở trạng thái vắng mặt cần xử lý.");
        }

        if (!accessSupport.isCurrentAdmin()) {
            accessSupport.ensureCurrentDepartmentCanManageActivity(registration.getActivity());
        }

        Users reviewer = accessSupport.requireCurrentUser();
        boolean firstAbsenceReview = !Boolean.TRUE.equals(registration.getAbsenceReviewed());
        LocalDateTime processedAt = LocalDateTime.now();
        registration.setAbsenceReviewed(true);
        registration.setAbsenceReviewedBy(reviewer.getId());
        registration.setAbsenceReviewedAt(processedAt);
        registration.setAbsenceReviewNote(request != null ? request.getNote() : null);

        Registrations saved = registrationRepository.save(registration);
        if (firstAbsenceReview) {
            String note = request != null && request.getNote() != null && !request.getNote().isBlank()
                    ? " Nội dung xử lý: " + request.getNote().trim()
                    : "";
            notifyAbsenceViolationProcessed(
                    saved,
                    ABSENCE_REVIEW_PROCESSED_REFERENCE_TYPE,
                    "BTC đã xử lý trường hợp vắng mặt",
                    "BTC đã xử lý trường hợp vắng mặt của bạn trong hoạt động '"
                            + activityTitle(saved) + "'." + note,
                    processedAt);
        }
        return registrationMapper.toResponseWithProof(saved, resolveProofStatus(saved));
    }

    private void notifyAbsenceViolationProcessed(
            Registrations registration,
            String referenceType,
            String title,
            String message,
            LocalDateTime processedAt) {
        if (registration == null || registration.getStudent() == null || registration.getStudent().getId() == null
                || registration.getActivity() == null || registration.getActivity().getId() == null
                || processedAt == null) {
            return;
        }

        registrationKafkaProducer.sendAbsenceViolationProcessed(
                registration.getStudent().getId(),
                registration.getId(),
                registration.getActivity().getId(),
                activityTitle(registration),
                referenceType,
                title,
                message,
                processedAt);
    }

    private String activityTitle(Registrations registration) {
        return registration.getActivity() != null && registration.getActivity().getTitle() != null
                ? registration.getActivity().getTitle()
                : "Hoạt động";
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

    private void enrichFaceVerificationState(RegistrationResponse response, Registrations registration) {
        ActivitySchedule targetSchedule = resolveFaceVerificationTargetSchedule(response, registration);
        boolean legacyWithoutSchedule = registration.getRegisteredSchedules() == null
                || registration.getRegisteredSchedules().isEmpty();
        boolean hasAttemptScope = targetSchedule != null || legacyWithoutSchedule;

        long rawAttemptCount = hasAttemptScope
                ? countFaceAttempts(registration, targetSchedule)
                : 0;
        int attemptCount = rawAttemptCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rawAttemptCount;
        int remainingAttempts = Math.max(FACE_CHECK_IN_MAX_ATTEMPTS - attemptCount, 0);
        boolean cancelled = registration.getStatus() != null && registration.getStatus() == 2;
        boolean faceVerified = Integer.valueOf(1).equals(registration.getStatus())
                || isFaceScheduleVerified(registration, targetSchedule);
        boolean checkedIn = registration.getAttendances() != null
                && registration.getAttendances().stream().anyMatch(attendance -> attendance.getCheckinTime() != null);
        Optional<FaceCheckInAttempt> latestAttempt = hasAttemptScope
                ? findLatestFaceAttempt(registration, targetSchedule)
                : Optional.empty();
        boolean latestAttemptMatched = latestAttempt
                .map(this::isSuccessfulFaceAttempt)
                .orElse(false);
        boolean latestAttemptAllowsRetry = latestAttempt
                .map(attempt -> Boolean.TRUE.equals(attempt.getAllowRetry()))
                .orElse(false);
        boolean exhausted = !cancelled
                && !faceVerified
                && !latestAttemptMatched
                && attemptCount >= FACE_CHECK_IN_MAX_ATTEMPTS
                && !latestAttemptAllowsRetry;

        response.setFaceVerificationAttemptCount(attemptCount);
        response.setFaceVerificationMaxAttempts(FACE_CHECK_IN_MAX_ATTEMPTS);
        response.setFaceVerificationRemainingAttempts(remainingAttempts);
        response.setFaceVerificationExhausted(exhausted);
        response.setCanSubmitComplaint(!cancelled && (checkedIn || exhausted));

        if (exhausted) {
            response.setParticipationStatus("FACE_VERIFICATION_EXHAUSTED");
            response.setNextAction("SUBMIT_COMPLAINT");
        }
    }

    private boolean hasFaceVerifiedAttendance(Registrations registration) {
        return registration.getAttendances() != null
                && registration.getAttendances().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(attendance -> Integer.valueOf(Attendances.STATUS_FACE_VERIFIED)
                                .equals(attendance.getStatus()));
    }

    private ActivitySchedule resolveFaceVerificationTargetSchedule(
            RegistrationResponse response,
            Registrations registration) {
        List<ActivitySchedule> registeredSchedules = registration.getRegisteredSchedules() != null
                ? registration.getRegisteredSchedules()
                : List.of();
        if (registeredSchedules.isEmpty()) {
            return null;
        }

        List<Long> registeredScheduleIds = registeredSchedules.stream()
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .filter(Objects::nonNull)
                .toList();
        List<Attendances> attendances = registration.getAttendances() != null
                ? registration.getAttendances()
                : List.of();

        Optional<ActivitySchedule> checkedOutSchedule = attendances.stream()
                .filter(Objects::nonNull)
                .filter(attendance -> attendance.getSchedule() != null
                        && registeredScheduleIds.contains(attendance.getSchedule().getId()))
                .filter(this::isReadyForFaceVerification)
                .map(attendance -> attendance.getSchedule())
                .filter(Objects::nonNull)
                .min(Comparator.comparing(
                        (ActivitySchedule schedule) -> schedule.getStartTime(),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        if (checkedOutSchedule.isPresent()) {
            return checkedOutSchedule.get();
        }

        return registeredSchedules.stream()
                .filter(Objects::nonNull)
                .filter(schedule -> !isFaceScheduleVerified(registration, schedule))
                .filter(schedule -> isFaceAttemptExhausted(registration, schedule))
                .filter(Objects::nonNull)
                .min(Comparator.comparing(
                        (ActivitySchedule schedule) -> schedule.getStartTime(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean isReadyForFaceVerification(Attendances attendance) {
        return attendance.getCheckinTime() != null
                && attendance.getCheckoutTime() != null
                && !Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus())
                && !Integer.valueOf(Attendances.STATUS_ABSENT).equals(attendance.getStatus());
    }

    private boolean isFaceScheduleVerified(Registrations registration, ActivitySchedule schedule) {
        if (schedule == null) {
            return hasFaceVerifiedAttendance(registration);
        }
        return registration.getAttendances() != null
                && registration.getAttendances().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(attendance -> attendance.getSchedule() != null
                                && Objects.equals(attendance.getSchedule().getId(), schedule.getId())
                                && Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus()));
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

    private boolean isFaceAttemptExhausted(Registrations registration, ActivitySchedule schedule) {
        long attemptCount = countFaceAttempts(registration, schedule);
        if (attemptCount < FACE_CHECK_IN_MAX_ATTEMPTS) {
            return false;
        }
        return findLatestFaceAttempt(registration, schedule)
                .map(attempt -> !Boolean.TRUE.equals(attempt.getAllowRetry()) && !isSuccessfulFaceAttempt(attempt))
                .orElse(true);
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

    // ============ NEW METHODS FOR UC FEATURES ============

    @Override
    @Transactional
    public RegistrationResponse updateSessions(Long registrationId, List<Long> sessionIds) {
        Users student = getCurrentStudentForRegistration();

        Registrations registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy đăng ký"));

        // Verify ownership
        if (!registration.getStudent().getId().equals(student.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền chỉnh sửa");
        }

        // Cannot update if already attended
        if (registration.getStatus() == 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể điều chỉnh khi đã điểm danh");
        }

        List<ActivitySchedule> newSchedules = resolveSelectedSchedules(registration.getActivity(), sessionIds);
        ensureNoAttendanceProgress(registration);
        validateNoScheduleConflicts(student.getId(), registration.getActivity(), newSchedules);

        registration.setRegisteredSchedules(newSchedules);
        registration = registrationRepository.save(registration);
        syncPendingAttendances(registration, newSchedules);

        activityCacheService.evictActivityListCaches();
        return registrationMapper.toResponse(registration);
    }

    private void validateNoScheduleConflicts(
            Long studentId,
            Activities targetActivity,
            List<ActivitySchedule> selectedSchedules) {
        if (studentId == null || targetActivity == null || selectedSchedules == null || selectedSchedules.isEmpty()) {
            return;
        }

        List<Registrations> existingRegistrations = registrationRepository
                .findActiveRegistrationsWithSchedulesByStudentId(
                        studentId, Registrations.STATUS_CANCELLED);
        if (existingRegistrations == null || existingRegistrations.isEmpty()) {
            return;
        }

        for (ActivitySchedule selectedSchedule : selectedSchedules) {
            if (!hasValidScheduleTime(selectedSchedule)) {
                continue;
            }

            List<ActivitySchedule> overlappingSchedules = registrationRepository
                    .findOverlappingActiveSchedulesByStudentId(
                            studentId,
                            targetActivity.getId(),
                            selectedSchedule.getStartTime(),
                            selectedSchedule.getEndTime(),
                            Registrations.STATUS_CANCELLED);
            if (overlappingSchedules != null && !overlappingSchedules.isEmpty()) {
                ActivitySchedule existingSchedule = overlappingSchedules.get(0);
                throw new AppException(
                        ErrorCode.INVALID_ACTION,
                        buildScheduleConflictMessage(
                                targetActivity,
                                selectedSchedule,
                                existingSchedule.getActivity(),
                                existingSchedule));
            }

            for (Registrations existingRegistration : existingRegistrations) {
                if (existingRegistration == null
                        || existingRegistration.getActivity() == null
                        || Integer.valueOf(Registrations.STATUS_CANCELLED).equals(existingRegistration.getStatus())
                        || Objects.equals(existingRegistration.getActivity().getId(), targetActivity.getId())) {
                    continue;
                }

                List<ActivitySchedule> existingSchedules = existingRegistration.getRegisteredSchedules();
                if (existingSchedules == null || existingSchedules.isEmpty()) {
                    // Legacy registrations may not have a row in registration_schedules.
                    // Treat all detailed sessions of that activity as occupied so that
                    // missing legacy links cannot bypass the overlap rule.
                    existingSchedules = scheduleRepository.findByActivityId(existingRegistration.getActivity().getId());
                }

                for (ActivitySchedule existingSchedule : existingSchedules) {
                    if (hasOverlappingSchedule(selectedSchedule, existingSchedule)) {
                        throw new AppException(
                                ErrorCode.INVALID_ACTION,
                                buildScheduleConflictMessage(targetActivity, selectedSchedule,
                                        existingRegistration.getActivity(),
                                        existingSchedule));
                    }
                }
            }
        }
    }

    private boolean hasValidScheduleTime(ActivitySchedule schedule) {
        return schedule != null
                && schedule.getStartTime() != null
                && schedule.getEndTime() != null
                && schedule.getStartTime().isBefore(schedule.getEndTime());
    }

    private boolean hasOverlappingSchedule(ActivitySchedule left, ActivitySchedule right) {
        return hasValidScheduleTime(right)
                && left.getStartTime().isBefore(right.getEndTime())
                && left.getEndTime().isAfter(right.getStartTime());
    }

    private String buildScheduleConflictMessage(
            Activities targetActivity,
            ActivitySchedule selectedSchedule,
            Activities existingActivity,
            ActivitySchedule existingSchedule) {
        String selectedTitle = StringUtils.hasText(selectedSchedule.getTitle())
                ? selectedSchedule.getTitle()
                : "buổi đã chọn";
        String existingTitle = StringUtils.hasText(existingSchedule.getTitle())
                ? existingSchedule.getTitle()
                : "buổi đã đăng ký";
        String targetActivityTitle = StringUtils.hasText(targetActivity.getTitle())
                ? targetActivity.getTitle()
                : "hoạt động hiện tại";
        String existingActivityTitle = existingActivity != null && StringUtils.hasText(existingActivity.getTitle())
                ? existingActivity.getTitle()
                : "hoạt động khác";

        return String.format(
                "Buổi %s (%s - %s) của hoạt động %s bị trùng thời gian với %s (%s - %s) thuộc hoạt động %s. "
                        + "Vui lòng chọn buổi khác.",
                selectedTitle,
                formatScheduleTime(selectedSchedule.getStartTime()),
                formatScheduleTime(selectedSchedule.getEndTime()),
                targetActivityTitle,
                existingTitle,
                formatScheduleTime(existingSchedule.getStartTime()),
                formatScheduleTime(existingSchedule.getEndTime()),
                existingActivityTitle);
    }

    private String formatScheduleTime(LocalDateTime time) {
        return time == null
                ? "không xác định"
                : time.atOffset(ZoneOffset.UTC)
                        .atZoneSameInstant(DISPLAY_ZONE)
                        .format(SCHEDULE_TIME_FORMATTER);
    }

    private List<ActivitySchedule> resolveSelectedSchedules(Activities activity, List<Long> scheduleIds) {
        List<ActivitySchedule> activitySchedules = new ArrayList<>();
        List<ActivitySchedule> foundSchedules = scheduleRepository.findByActivityId(activity.getId());
        if (foundSchedules != null) {
            activitySchedules.addAll(foundSchedules);
        }
        boolean activityHasSessions = !activitySchedules.isEmpty();
        List<Long> distinctScheduleIds = scheduleIds == null
                ? List.of()
                : scheduleIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (!activityHasSessions) {
            if (!distinctScheduleIds.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này không có buổi chi tiết để đăng ký.");
            }
            return new ArrayList<>();
        }

        if (distinctScheduleIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Vui lòng chọn ít nhất một buổi tham gia.");
        }

        Map<Long, ActivitySchedule> schedulesById = activitySchedules.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(schedule -> schedule.getId(), schedule -> schedule));

        List<ActivitySchedule> selectedSchedules = new ArrayList<>();
        for (Long scheduleId : distinctScheduleIds) {
            ActivitySchedule schedule = schedulesById.get(scheduleId);
            if (schedule == null) {
                throw new AppException(ErrorCode.INVALID_ACTION,
                        "Danh sách buổi không hợp lệ hoặc không thuộc hoạt động này.");
            }
            selectedSchedules.add(schedule);
        }
        return selectedSchedules;
    }

    private void syncPendingAttendances(Registrations registration, List<ActivitySchedule> selectedSchedules) {
        if (selectedSchedules == null || selectedSchedules.isEmpty()) {
            return;
        }

        List<Attendances> existingAttendances = attendanceRepository.findAllByRegistrationId(registration.getId());
        Set<Long> selectedScheduleIds = selectedSchedules.stream()
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .collect(Collectors.toSet());
        Set<Long> existingScheduleIds = existingAttendances.stream()
                .map(attendance -> attendance.getSchedule())
                .filter(Objects::nonNull)
                .map(schedule -> schedule.getId())
                .collect(Collectors.toSet());

        List<Attendances> toDelete = existingAttendances.stream()
                .filter(attendance -> attendance.getSchedule() != null)
                .filter(attendance -> !selectedScheduleIds.contains(attendance.getSchedule().getId()))
                .filter(attendance -> attendance.getCheckinTime() == null
                        && attendance.getCheckoutTime() == null
                        && (attendance.getStatus() == null
                                || Integer.valueOf(Attendances.STATUS_PENDING).equals(attendance.getStatus())))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            attendanceRepository.deleteAll(toDelete);
        }

        List<Attendances> toCreate = selectedSchedules.stream()
                .filter(Objects::nonNull)
                .filter(schedule -> !existingScheduleIds.contains(schedule.getId()))
                .map(schedule -> Attendances.builder()
                        .registration(registration)
                        .schedule(schedule)
                        .status(Attendances.STATUS_PENDING)
                        .build())
                .collect(Collectors.toList());

        if (!toCreate.isEmpty()) {
            attendanceRepository.saveAll(toCreate);
        }
    }

    private void ensureNoAttendanceProgress(Registrations registration) {
        boolean hasProgress = attendanceRepository.findAllByRegistrationId(registration.getId()).stream()
                .anyMatch(attendance -> attendance.getCheckinTime() != null
                        || attendance.getCheckoutTime() != null
                        || (attendance.getStatus() != null
                                && !Integer.valueOf(Attendances.STATUS_PENDING).equals(attendance.getStatus())));
        if (hasProgress) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không thể điều chỉnh buổi khi đã phát sinh điểm danh.");
        }
    }

}
