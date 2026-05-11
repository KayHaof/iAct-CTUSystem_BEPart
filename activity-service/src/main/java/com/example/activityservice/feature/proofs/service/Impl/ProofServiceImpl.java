package com.example.activityservice.feature.proofs.service.Impl;

import com.example.activityservice.feature.proofs.dto.ProofResponse;
import com.example.activityservice.feature.proofs.dto.ProofSubmissionRequest;
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

@Service
@RequiredArgsConstructor
public class ProofServiceImpl implements ProofService {

    private final ProofRepository proofRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ProofMapper proofMapper;

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
        return proofMapper.toResponse(proofToSave);
    }
}