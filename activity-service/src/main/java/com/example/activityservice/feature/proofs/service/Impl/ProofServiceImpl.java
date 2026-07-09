package com.example.activityservice.feature.proofs.service.Impl;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
import com.example.activityservice.feature.points.service.PointCacheService;
import com.example.activityservice.feature.points.kafka.PointEventProducer;
import com.example.activityservice.feature.proofs.kafka.ProofEventProducer;
import com.example.activityservice.feature.proofs.mapper.ProofMapper;
import com.example.activityservice.feature.proofs.model.Proofs;
import com.example.activityservice.feature.users.model.Users;
import com.example.activityservice.feature.users.repository.UserRepository;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activities.repository.ActivityRepository;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.attendances.repository.AttendanceRepository;
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

@Service
@RequiredArgsConstructor
public class ProofServiceImpl implements ProofService {

        private final ProofRepository proofRepository;
        private final RegistrationRepository registrationRepository;
        private final ActivityRepository activityRepository;
        private final UserRepository userRepository;
        private final AttendanceRepository attendanceRepository;
        private final ProofMapper proofMapper;
        private final ProofEventProducer proofEventProducer;
        private final PointEventProducer pointEventProducer;
        private final PointCacheService pointCacheService;

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
                Activities activity = activityRepository.findById(request.getActivityId())
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Hoạt động không tồn tại!"));

                // 2. Kiểm tra xem sinh viên đã đăng ký và ĐÃ ĐIỂM DANH chưa?
                Registrations reg = registrationRepository
                                .findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                                                "Bạn chưa đăng ký hoạt động này!"));

                // Trạng thái (status) = 1 nghĩa là đã quét mã Check-in/Check-out.
                // Nếu chưa quét (0) hoặc đã hủy (2) thì không cho nộp!
                if (reg.getStatus() != 1) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Bạn phải quét mã điểm danh tham gia hoạt động trước khi nộp minh chứng!");
                }

                // 3. Xử lý nộp/cập nhật minh chứng
                Attendances attendance = attendanceRepository.findByRegistrationId(reg.getId())
                                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION,
                                                "Ban phai check-in tham gia hoat dong truoc khi nop minh chung!"));

                if (attendance.getCheckinTime() == null) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Ban phai check-in tham gia hoat dong truoc khi nop minh chung!");
                }

                if (attendance.getCheckoutTime() == null) {
                        throw new AppException(ErrorCode.INVALID_ACTION,
                                        "Ban phai check-out sau khi tham gia hoat dong truoc khi nop minh chung!");
                }

                Proofs existingProof = proofRepository.findByRegistrationId(reg.getId())
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

                if (activityId != null && status != null) {
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
        @Transactional
        public ProofResponse approveProof(Long proofId) {
                Proofs proof = proofRepository.findById(proofId)
                                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED,
                                                "Minh chung khong ton tai!"));
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
                                                "Minh chung khong ton tai!"));
                proof.setStatus(2);
                proof.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Minh chung khong hop le");
                proof.setVerifiedBy(getCurrentReviewerId());
                proof.setVerifiedTime(LocalDateTime.now());
                Proofs savedProof = proofRepository.save(proof);

                proofEventProducer.publishRejected(savedProof);
                pointEventProducer.publishRevoked(savedProof.getStudentId(), savedProof.getActivity(),
                                savedProof.getRejectionReason());
                evictPointCaches(savedProof);
                return proofMapper.toResponse(savedProof);
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
}
