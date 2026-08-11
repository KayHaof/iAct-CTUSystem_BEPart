package com.example.activityservice.feature.proofs.service.Impl;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.activityservice.feature.proofs.dto.ProofActivitySummaryResponse;
import com.example.activityservice.feature.proofs.dto.ProofStatusResponse;
import com.example.activityservice.feature.points.service.PointCacheService;
import com.example.activityservice.feature.points.kafka.PointEventProducer;
import com.example.activityservice.feature.proofs.kafka.ProofEventProducer;
import com.example.activityservice.feature.proofs.mapper.ProofMapper;
import com.example.activityservice.feature.proofs.model.Proofs;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.activities.service.impl.ActivityAccessSupport;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
import com.example.activityservice.feature.attendances.service.AttendanceService;
import com.example.activityservice.feature.proofs.repository.ProofRepository;
import com.example.activityservice.feature.proofs.service.ProofService;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import com.example.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProofServiceImpl implements ProofService {

        private final ProofRepository proofRepository;
        private final RegistrationRepository registrationRepository;
        private final ActivityRepository activityRepository;
        private final UserRepository userRepository;
        private final AttendanceRepository attendanceRepository;
        private final AttendanceService attendanceService;
        private final ProofMapper proofMapper;
        private final ProofEventProducer proofEventProducer;
        private final PointEventProducer pointEventProducer;
        private final PointCacheService pointCacheService;
        private final ActivityAccessSupport accessSupport;

        private Users getCurrentStudent() {
                String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                                .getName();
                return userRepository.findByUsername(username)
                                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        @Override
        @Transactional
        public ProofResponse submitProof(ProofSubmissionRequest request) {
                Users student = getCurrentStudent();
                // 1. Kiểm tra xem hoạt động có tồn tại không
                activityRepository.findById(request.getActivityId())
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Hoạt động không tồn tại!"));

                // 2. Kiểm tra xem sinh viên đã đăng ký và ĐÃ ĐIỂM DANH chưa?
                Registrations reg = registrationRepository
                                .findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                                                "Bạn chưa đăng ký hoạt động này!"));

                // Trạng thái (status) = 1 nghĩa là sinh viên đã xác thực khuôn mặt thành công.
                // Nếu chưa xác thực (0) hoặc đã hủy (2) thì không cho nộp!
                if (reg.getStatus() != 1) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải xác thực khuôn mặt tham gia hoạt động trước khi nộp minh chứng!");
                }

                // 3. Xử lý nộp/cập nhật minh chứng
                ensureAllRegisteredSessionsFaceVerified(reg);
                Attendances attendance = attendanceRepository.findFirstByRegistrationIdOrderByCheckinTimeAscIdAsc(reg.getId())
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                        "Bạn phải xác thực khuôn mặt tham gia hoạt động trước khi nộp minh chứng!"));

                if (attendance.getCheckinTime() == null) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải xác thực khuôn mặt tham gia hoạt động trước khi nộp minh chứng!");
                }

                if (attendance.getCheckoutTime() == null) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải check-out trước khi xác thực khuôn mặt và nộp minh chứng!");
                }

                Proofs existingProof = proofRepository.findFirstByRegistrationIdOrderByCreatedAtDescIdDesc(reg.getId())
                                .orElse(null);

                Proofs proofToSave;
                if (existingProof != null) {
                        if (existingProof.getStatus() == 1) {
                                throw new AppException(ErrorCode.INVALID_ACTION,
                                                "Minh chứng của bạn đã được duyệt, không thể sửa đổi!");
                        }
                        proofToSave = existingProof;
                        proofMapper.updateEntityFromRequest(request, proofToSave);
                        proofToSave.setStatus(0);
                } else {
                        proofToSave = proofMapper.toNewEntity(request, reg);
                        proofToSave.setStatus(0);
                }

                proofToSave = proofRepository.save(proofToSave);
                proofEventProducer.publishSubmitted(proofToSave);
                return proofMapper.toResponse(proofToSave);
        }

        @Override
        @Transactional(readOnly = true)
        public PageDTO<ProofResponse> getProofs(Integer status, Long activityId, Pageable pageable) {
                Page<Proofs> page;

                if (accessSupport.isCurrentDepartment()) {
                        Long departmentId = accessSupport.requireCurrentDepartmentId();
                        page = getDepartmentProofs(status, activityId, departmentId, pageable);
                } else if (activityId != null && status != null) {
                        page = proofRepository.findByRegistration_Activity_IdAndStatus(activityId, status, pageable);
                } else if (activityId != null) {
                        page = proofRepository.findByRegistration_Activity_Id(activityId, pageable);
                } else if (status != null) {
                        page = proofRepository.findByStatus(status, pageable);
                } else {
                        page = proofRepository.findAll(pageable);
                }

                return new PageDTO<>(
                                page,
                                page.getContent().stream()
                                                .map(proofMapper::toResponse)
                                                .toList());
        }

        @Override
        @Transactional(readOnly = true)
        public PageDTO<ProofResponse> getSubmittedStudents(Long activityId, Integer status, Pageable pageable) {
                return getProofs(status, activityId, pageable);
        }

        @Override
        @Transactional
        public ProofActivitySummaryResponse getActivitySummary(Long activityId) {
                ensureCanManageActivity(activityId);
                attendanceService.recordExpiredAbsences();

                return ProofActivitySummaryResponse.builder()
                                .activityId(activityId)
                                .totalRegisteredStudents(registrationRepository.countActiveRegistrationsByActivityId(activityId))
                                .totalEligibleStudents(registrationRepository.countByActivityIdAndStatus(
                                                activityId, Registrations.STATUS_ATTENDED))
                                .totalSubmittedProofs(proofRepository.countByRegistration_Activity_Id(activityId))
                                .totalSubmittedStudents(proofRepository.countDistinctStudentsByActivityId(activityId))
                                .totalNotSubmittedEligibleStudents(registrationRepository.countEligibleRegistrationsWithoutProof(activityId))
                                .pendingProofs(proofRepository.countByRegistration_Activity_IdAndStatus(activityId, 0))
                                .approvedProofs(proofRepository.countByRegistration_Activity_IdAndStatus(activityId, 1))
                                .rejectedProofs(proofRepository.countByRegistration_Activity_IdAndStatus(activityId, 2))
                                .absentStudents(registrationRepository.countAbsentRegistrationsByActivityId(activityId))
                                .unreviewedAbsentStudents(registrationRepository.countUnreviewedAbsencesByActivityId(activityId))
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public ProofStatusResponse getMyProofStatus(Long activityId) {
                Users student = getCurrentStudent();
                Registrations registration = registrationRepository
                                .findByStudentIdAndActivityId(student.getId(), activityId)
                                .orElse(null);

                if (registration == null) {
                        return ProofStatusResponse.builder()
                                        .activityId(activityId)
                                        .proofStatus(0)
                                        .submitted(false)
                                        .canSubmit(false)
                                        .canResubmit(false)
                                        .attendanceStatus("NOT_REGISTERED")
                                        .build();
                }

                Proofs proof = proofRepository.findFirstByRegistrationIdOrderByCreatedAtDescIdDesc(registration.getId())
                                .orElse(null);
                int proofStatus = toStudentProofStatus(proof);
                boolean eligible = Integer.valueOf(Registrations.STATUS_ATTENDED).equals(registration.getStatus());

                return ProofStatusResponse.builder()
                                .activityId(activityId)
                                .registrationId(registration.getId())
                                .registrationStatus(registration.getStatus())
                                .attendanceStatus(resolveAttendanceStatus(registration))
                                .proofStatus(proofStatus)
                                .submitted(proof != null)
                                .canSubmit(eligible && (proof == null || proofStatus == 1 || proofStatus == 3))
                                .canResubmit(eligible && proofStatus == 3)
                                .proofId(proof != null ? proof.getId() : null)
                                .rejectionReason(proof != null ? proof.getRejectionReason() : null)
                                .submittedAt(proof != null ? proof.getCreatedAt() : null)
                                .updatedAt(proof != null ? proof.getUpdatedAt() : null)
                                .build();
        }

        @Override
        @Transactional
        public ProofResponse resubmitProof(Long proofId, ProofSubmissionRequest request) {
                Users student = getCurrentStudent();
                Proofs proof = proofRepository.findById(proofId)
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Minh chứng không tồn tại!"));

                if (proof.getRegistration() == null
                                || proof.getRegistration().getStudent() == null
                                || !Objects.equals(proof.getRegistration().getStudent().getId(), student.getId())) {
                        throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền cập nhật minh chứng này.");
                }
                if (!Integer.valueOf(2).equals(proof.getStatus())) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Chỉ minh chứng bị từ chối mới được nộp lại.");
                }
                if (!Objects.equals(proof.getActivity() != null ? proof.getActivity().getId() : null,
                                request.getActivityId())) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Minh chứng không thuộc hoạt động đang cập nhật.");
                }

                Registrations registration = validateProofSubmission(student, request);
                proofMapper.updateEntityFromRequest(request, proof);
                proof.setRegistration(registration);
                proof.setStatus(0);
                proof.setRejectionReason(null);
                proof.setVerifiedBy(null);
                proof.setVerifiedTime(null);

                Proofs savedProof = proofRepository.save(proof);
                proofEventProducer.publishSubmitted(savedProof);
                return proofMapper.toResponse(savedProof);
        }

        @Override
        @Transactional
        public ProofResponse approveProof(Long proofId) {
                Proofs proof = proofRepository.findById(proofId)
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Minh chứng không tồn tại!"));
                ensureCanReviewProof(proof);
                proof.setStatus(1);
                proof.setRejectionReason(null);
                proof.setVerifiedBy(getCurrentReviewerId());
                proof.setVerifiedTime(LocalDateTime.now());
                Proofs savedProof = proofRepository.save(proof);

                proofEventProducer.publishApproved(savedProof);
                pointEventProducer.publishAwarded(savedProof.getStudentId(), savedProof.getActivity());
                evictPointCaches(savedProof);
                return proofMapper.toResponse(savedProof);
        }

        @Override
        @Transactional
        public ProofResponse rejectProof(Long proofId, String reason) {
                Proofs proof = proofRepository.findById(proofId)
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Minh chứng không tồn tại!"));
                ensureCanReviewProof(proof);
                proof.setStatus(2);
                proof.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Minh chứng không hợp lệ");
                proof.setVerifiedBy(getCurrentReviewerId());
                proof.setVerifiedTime(LocalDateTime.now());
                Proofs savedProof = proofRepository.save(proof);

                proofEventProducer.publishRejected(savedProof);
                pointEventProducer.publishRevoked(savedProof.getStudentId(), savedProof.getActivity(),
                                savedProof.getRejectionReason());
                evictPointCaches(savedProof);
                return proofMapper.toResponse(savedProof);
        }

        private Page<Proofs> getDepartmentProofs(
                        Integer status,
                        Long activityId,
                        Long departmentId,
                        Pageable pageable) {
                if (activityId != null && status != null) {
                        return proofRepository.findByActivityIdAndActivityDepartmentIdAndStatus(
                                        activityId, departmentId, status, pageable);
                }
                if (activityId != null) {
                        return proofRepository.findByActivityIdAndActivityDepartmentId(
                                        activityId, departmentId, pageable);
                }
                if (status != null) {
                        return proofRepository.findByActivityDepartmentIdAndStatus(departmentId, status, pageable);
                }
                return proofRepository.findByActivityDepartmentId(departmentId, pageable);
        }

        private Registrations validateProofSubmission(Users student, ProofSubmissionRequest request) {
                activityRepository.findById(request.getActivityId())
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Hoạt động không tồn tại!"));

                Registrations registration = registrationRepository
                                .findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                                                "Bạn chưa đăng ký hoạt động này!"));

                if (!Integer.valueOf(Registrations.STATUS_ATTENDED).equals(registration.getStatus())) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải hoàn tất điểm danh trước khi nộp minh chứng!");
                }

                ensureAllRegisteredSessionsFaceVerified(registration);
                Attendances attendance = attendanceRepository
                                .findFirstByRegistrationIdOrderByCheckinTimeAscIdAsc(registration.getId())
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải check-in, check-out và xác thực khuôn mặt trước khi nộp minh chứng!"));
                if (attendance.getCheckinTime() == null || attendance.getCheckoutTime() == null) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải check-in và check-out trước khi nộp minh chứng!");
                }
                return registration;
        }

        private int toStudentProofStatus(Proofs proof) {
                if (proof == null || proof.getStatus() == null) {
                        return 0;
                }
                if (proof.getStatus() == 0) {
                        return 1;
                }
                if (proof.getStatus() == 1) {
                        return 2;
                }
                if (proof.getStatus() == 2) {
                        return 3;
                }
                return 0;
        }

        private String resolveAttendanceStatus(Registrations registration) {
                if (Integer.valueOf(Registrations.STATUS_ATTENDED).equals(registration.getStatus())) {
                        return "FACE_VERIFIED";
                }
                if (Integer.valueOf(Registrations.STATUS_ABSENT).equals(registration.getStatus())) {
                        return "ABSENT";
                }
                return "NOT_READY";
        }

        private void ensureCanManageActivity(Long activityId) {
                Activities activity = activityRepository.findById(activityId)
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Hoạt động không tồn tại!"));
                if (accessSupport.isCurrentAdmin()) {
                        return;
                }
                if (accessSupport.isCurrentDepartment()) {
                        accessSupport.ensureCurrentDepartmentCanManageActivity(activity);
                        return;
                }
                throw new AppException(ErrorCode.FORBIDDEN,
                                "Bạn không có quyền xem minh chứng của hoạt động này.");
        }

        private void ensureCanReviewProof(Proofs proof) {
                if (accessSupport.isCurrentAdmin()) {
                        return;
                }
                if (accessSupport.isCurrentDepartment()) {
                        accessSupport.ensureCurrentDepartmentCanManageActivity(proof.getActivity());
                        return;
                }
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền duyệt minh chứng này.");
        }

        private void evictPointCaches(Proofs proof) {
                Long semesterId = proof.getActivity() != null && proof.getActivity().getSemester() != null
                                ? proof.getActivity().getSemester().getId()
                                : null;
                pointCacheService.evictStudentPointCaches(proof.getStudentId(), semesterId);
        }

        private Long getCurrentReviewerId() {
                String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication())
                                .getName();
                return userRepository.findByUsername(username)
                                .map(user -> user != null ? user.getId() : null)
                                .orElse(null);
        }

        private void ensureAllRegisteredSessionsFaceVerified(Registrations registration) {
                List<Attendances> attendances = attendanceRepository.findAllByRegistrationId(registration.getId());
                List<ActivitySchedule> registeredSchedules = registration.getRegisteredSchedules() != null
                                ? registration.getRegisteredSchedules()
                                : List.of();

                if (registeredSchedules.isEmpty()) {
                        boolean completedLegacyAttendance = attendances.stream()
                                        .anyMatch(attendance -> attendance.getSchedule() == null
                                                        && Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus())
                                                        && attendance.getCheckinTime() != null
                                                        && attendance.getCheckoutTime() != null);
                        if (!completedLegacyAttendance) {
                                throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải check-in, check-out và xác thực khuôn mặt trước khi nộp minh chứng!");
                        }
                        return;
                }

                Set<Long> verifiedScheduleIds = attendances.stream()
                                .filter(attendance -> Integer.valueOf(Attendances.STATUS_FACE_VERIFIED).equals(attendance.getStatus()))
                                .filter(attendance -> attendance.getCheckinTime() != null && attendance.getCheckoutTime() != null)
                                .map(attendance -> attendance.getSchedule())
                                .filter(Objects::nonNull)
                                .map(schedule -> schedule.getId())
                                .collect(Collectors.toSet());

                boolean completedAll = registeredSchedules.stream()
                                .filter(Objects::nonNull)
                                .map(schedule -> schedule.getId())
                                .allMatch(verifiedScheduleIds::contains);

                if (!completedAll) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                "Bạn phải hoàn tất check-in, check-out và xác thực khuôn mặt cho tất cả buổi đã đăng ký.");
                }
        }
}
