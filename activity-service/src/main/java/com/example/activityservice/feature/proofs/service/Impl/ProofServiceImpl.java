package com.example.activityservice.feature.proofs.service.Impl;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
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
import com.example.activityservice.feature.proofs.repository.ProofRepository;
import com.example.activityservice.feature.proofs.service.ProofService;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.registration.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
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
    private final ProofMapper proofMapper;
    private final ProofEventProducer proofEventProducer;
    private final PointEventProducer pointEventProducer;

    private Users getCurrentStudent() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Override
    @Transactional
    public ProofResponse submitProof(ProofSubmissionRequest request) {
        Users student = getCurrentStudent();
        // 1. Kiểm tra xem hoạt động có tồn tại không
        Activities activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Hoạt động không tồn tại!"));

        // 2. Kiểm tra xem sinh viên đã đăng ký và ĐÃ ĐIỂM DANH chưa?
        Registrations reg = registrationRepository
                .findByStudentIdAndActivityId(student.getId(), request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACTION, "Bạn chưa đăng ký hoạt động này!"));

        // Trạng thái (status) = 1 nghĩa là đã quét mã Check-in/Check-out.
        // Nếu chưa quét (0) hoặc đã hủy (2) thì không cho nộp!
        if (reg.getStatus() != 1) {
            throw new AppException(ErrorCode.INVALID_ACTION,
                    "Bạn phải quét mã điểm danh tham gia hoạt động trước khi nộp minh chứng!");
        }

        // 3. Xử lý nộp/cập nhật minh chứng
        Proofs existingProof = proofRepository.findByStudentIdAndActivityId(student.getId(), request.getActivityId())
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
            proofToSave = proofMapper.toNewEntity(request, student.getId(), activity);
            proofToSave.setStatus(0);
        }

        proofToSave = proofRepository.save(proofToSave);
        proofEventProducer.publishSubmitted(proofToSave);
        return proofMapper.toResponse(proofToSave);
    }

    @Override
    @Transactional
    public ProofResponse approveProof(Long proofId) {
        Proofs proof = proofRepository.findById(proofId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Minh chung khong ton tai!"));
        proof.setStatus(1);
        proof.setRejectionReason(null);
        proof.setVerifiedBy(getCurrentReviewerId());
        proof.setVerifiedTime(LocalDateTime.now());
        Proofs savedProof = proofRepository.save(proof);

        proofEventProducer.publishApproved(savedProof);
        pointEventProducer.publishAwarded(savedProof.getStudentId(), savedProof.getActivity());
        return proofMapper.toResponse(savedProof);
    }

    @Override
    @Transactional
    public ProofResponse rejectProof(Long proofId, String reason) {
        Proofs proof = proofRepository.findById(proofId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Minh chung khong ton tai!"));
        proof.setStatus(2);
        proof.setRejectionReason(reason != null && !reason.isBlank() ? reason : "Minh chung khong hop le");
        proof.setVerifiedBy(getCurrentReviewerId());
        proof.setVerifiedTime(LocalDateTime.now());
        Proofs savedProof = proofRepository.save(proof);

        proofEventProducer.publishRejected(savedProof);
        pointEventProducer.publishRevoked(savedProof.getStudentId(), savedProof.getActivity(), savedProof.getRejectionReason());
        return proofMapper.toResponse(savedProof);
    }

    private Long getCurrentReviewerId() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByUsername(username)
                .map(Users::getId)
                .orElse(null);
    }
}
