package com.example.feature.registration.service.Impl;

import com.example.common.dto.ProfileDto;
import com.example.common.entity.Users;
import com.example.common.repository.LocalUserRepository;
import com.example.dto.ApiResponse;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.feature.proofs.model.Proofs;
import com.example.feature.proofs.repository.ProofRepository;
import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.mapper.RegistrationMapper;
import com.example.feature.registration.model.Registrations;
import com.example.feature.registration.repository.RegistrationRepository;
import com.example.feature.registration.service.RegistrationService;
import com.example.feignClient.IdentityServiceClient;
import com.example.feignClient.ProfileClient;
import com.example.service.ExcelExportService;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityScheduleRepository scheduleRepository;
    private final LocalUserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final ExcelExportService excelExportService;
    private final ProofRepository proofRepository;
    private final ProfileClient profileClient;
    private final IdentityServiceClient identityServiceClient;

    // --- Lấy sinh viên đang đăng nhập ---
    public Users getCurrentStudent() {
        Jwt jwt = (Jwt) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        assert jwt != null;
        String username = jwt.getClaimAsString("preferred_username");

        return userRepository.findByUsername(username).orElseGet(() -> {
            log.warn("User {} chưa có trong Activity DB. Tiến hành Lazy Sync...", username);

            try {
                var response = identityServiceClient.getUserByUsername(username).getResult();

                Users newUser = new Users();
                newUser.setId(response.getId());
                newUser.setUsername(response.getUsername());
                newUser.setEmail(response.getEmail());

                return userRepository.save(newUser);
            } catch (Exception e) {
                log.error("Không thể Lazy Sync user từ Identity", e);
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }
        });
    }

    private void populateProfileData(List<RegistrationResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<Long> userIds = responses.stream()
                .map(RegistrationResponse::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        try {
            ApiResponse<Map<Long, ProfileDto>> batchRes = profileClient.getProfilesBatch(userIds);
            if (batchRes != null && batchRes.getResult() != null) {
                Map<Long, ProfileDto> profileMap = batchRes.getResult();

                for (RegistrationResponse res : responses) {
                    ProfileDto p = profileMap.get(res.getStudentId());
                    if (p != null) {
                        res.setStudentName(p.getFullName());
                        res.setStudentCode(p.getStudentCode());
                        res.setAvatarUrl(p.getAvatarUrl());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Profile Service lấy data hàng loạt: {}", e.getMessage());
        }
    }

    // --- Xử lý bộ lọc tìm kiếm ---
    private Specification<Registrations> buildFilterSpecification(Long activityId, String status, List<Long> searchedUserIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("activity").get("id"), activityId));

            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                try {
                    int statusCode = Integer.parseInt(status);
                    predicates.add(cb.equal(root.get("status"), statusCode));
                } catch (NumberFormatException ignored) {}
            }

            if (searchedUserIds != null) {
                if (searchedUserIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    // [ĐÃ SỬA CHUẨN]: Trả lại get("student").get("id") vì student là Entity
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
                .map(registrationMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        Users student = getCurrentStudent();

        Activities activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Hoạt động không tồn tại"));

        if (activity.getStatus() != 1) throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này hiện chưa mở đăng ký.");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistrationStart()) || now.isAfter(activity.getRegistrationEnd()))
            throw new AppException(ErrorCode.INVALID_ACTION, "Rất tiếc, đã hết hoặc chưa tới thời hạn đăng ký.");

        Registrations existingReg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activity.getId()).orElse(null);
        if (existingReg != null && (existingReg.getStatus() == 0 || existingReg.getStatus() == 1))
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã đăng ký hoạt động này rồi nha!");
        if (registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2) >= activity.getMaxParticipants())
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này đã full chỗ mất rồi!");

        List<ActivitySchedule> selectedSchedules = new ArrayList<>();
        if (request.getScheduleIds() != null && !request.getScheduleIds().isEmpty()) {
            selectedSchedules = scheduleRepository.findAllById(request.getScheduleIds());
            if (selectedSchedules.size() != request.getScheduleIds().size()) throw new AppException(ErrorCode.INVALID_ACTION, "Danh sách buổi không hợp lệ!");
        }

        Registrations regToSave;
        if (existingReg != null && existingReg.getStatus() == 2) {
            regToSave = existingReg;
            registrationMapper.reRegisterEntity(regToSave, selectedSchedules);
        } else {
            // [ĐÃ SỬA CHUẨN]: Phải truyền nguyên object Users student, không phải student.getId()
            regToSave = registrationMapper.toNewEntity(student, activity, selectedSchedules);
        }

        return registrationMapper.toResponse(registrationRepository.save(regToSave));
    }

    @Override
    @Transactional
    public RegistrationResponse cancelByActivityId(Long activityId, String reason) {
        Users student = getCurrentStudent();
        Registrations reg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Bạn chưa đăng ký hoạt động này nên không hủy được!"));
        return this.processCancellation(reg, reason);
    }

    @Override
    @Transactional
    public RegistrationResponse cancel(Long regId, String reason) {
        Registrations reg = registrationRepository.findById(regId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED));
        return this.processCancellation(reg, reason);
    }

    private RegistrationResponse processCancellation(Registrations reg, String reason) {
        if (reg.getStatus() == 1) throw new AppException(ErrorCode.INVALID_ACTION, "Không được hủy khi đã điểm danh!");
        if (reg.getStatus() == 2) throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hủy đăng ký trước đó rồi!");

        registrationMapper.cancelEntity(reg, reason);

        return registrationMapper.toResponse(registrationRepository.save(reg));
    }

    @Override
    public PageDTO<RegistrationResponse> getParticipants(Long activityId, String keyword, String status, Pageable pageable) {
        List<Long> searchedUserIds = null;

        if (StringUtils.hasText(keyword)) {
            try {
                ApiResponse<List<Long>> res = profileClient.searchUserIdsByCriteria(keyword);
                searchedUserIds = (res != null && res.getResult() != null) ? res.getResult() : new ArrayList<>();
            } catch (Exception e) {
                log.warn("Lỗi khi tìm user ID từ Profile Service");
                searchedUserIds = new ArrayList<>();
            }
        }

        Specification<Registrations> spec = buildFilterSpecification(activityId, status, searchedUserIds);
        Page<Registrations> pageData = registrationRepository.findAll(spec, pageable);

        List<RegistrationResponse> dtoList = pageData.getContent().stream()
                .map(registrationMapper::toResponse)
                .collect(Collectors.toList());

        populateProfileData(dtoList);

        return PageDTO.<RegistrationResponse>builder()
                .pageNumber(pageable.getPageNumber() + 1)
                .totalPage(pageData.getTotalPages())
                .totalRows(pageData.getTotalElements())
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional
    public RegistrationResponse updateStatus(Long id, Integer status) {
        Registrations reg = registrationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Không tìm thấy đơn đăng ký!"));

        if (status == 2) {
            registrationMapper.cancelEntity(reg, "Quản trị viên / Khoa hủy đăng ký");
        } else {
            reg.setStatus(status);
        }

        return registrationMapper.toResponse(registrationRepository.save(reg));
    }

    @Override
    public void exportToExcel(Long activityId, String keyword, String status, OutputStream outputStream) {
        List<Long> searchedUserIds = null;
        if (StringUtils.hasText(keyword)) {
            try {
                ApiResponse<List<Long>> res = profileClient.searchUserIdsByCriteria(keyword);
                searchedUserIds = (res != null && res.getResult() != null) ? res.getResult() : new ArrayList<>();
            } catch (Exception e) {
                searchedUserIds = new ArrayList<>();
            }
        }

        Specification<Registrations> spec = buildFilterSpecification(activityId, status, searchedUserIds);
        List<Registrations> list = registrationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "registeredAt"));

        List<RegistrationResponse> dtoList = list.stream().map(registrationMapper::toResponse).collect(Collectors.toList());
        populateProfileData(dtoList);

        String[] headers = {"STT", "MSSV", "Họ và Tên", "Thời gian ĐK", "Buổi đăng ký", "Trạng thái", "Lý do hủy"};
        java.util.concurrent.atomic.AtomicInteger stt = new java.util.concurrent.atomic.AtomicInteger(1);
        try {
            excelExportService.export(
                    "Danh_sach_SV",
                    headers,
                    dtoList,
                    (dto) -> {
                        String statusStr = dto.getStatus() == 0 ? "Đã đăng ký" : (dto.getStatus() == 1 ? "Đã tham gia" : "Đã hủy");

                        String schedulesStr = "";
                        if (dto.getScheduleIds() != null && !dto.getScheduleIds().isEmpty()) {
                            schedulesStr = dto.getScheduleIds().size() + " buổi đăng ký";
                        }

                        return new Object[]{
                                stt.getAndIncrement(),
                                dto.getStudentCode() != null ? dto.getStudentCode() : "",
                                dto.getStudentName() != null ? dto.getStudentName() : "",
                                dto.getRegisteredAt(),
                                schedulesStr,
                                statusStr,
                                dto.getCancelReason() != null ? dto.getCancelReason() : ""
                        };
                    },
                    outputStream
            );
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

            // Lọc theo Sinh viên hiện tại (Đúng)
            predicates.add(cb.equal(root.get("student").get("id"), student.getId()));

            // [CHỖ CẦN SỬA]: Chọc thẳng vào semesterId của Activities
            if (semesterId != null) {
                // Nếu trong Activities.java ní đặt tên là semesterId thì dùng tên đó
                predicates.add(cb.equal(root.get("activity").get("semesterId"), semesterId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Registrations> myRecords = registrationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "registeredAt"));

        List<RegistrationResponse> responseList = myRecords.stream().map(reg -> {
            int proofStatus = 0;
            if (reg.getStatus() == 1) {
                Optional<Proofs> proofOpt = proofRepository.findByStudentIdAndActivityId(student.getId(), reg.getActivity().getId());
                if (proofOpt.isPresent()) {
                    Integer pStatus = proofOpt.get().getStatus();
                    if (pStatus == 0) proofStatus = 1;
                    else if (pStatus == 1) proofStatus = 2;
                    else if (pStatus == 2) proofStatus = 3;
                }
            }
            return registrationMapper.toResponseWithProof(reg, proofStatus);
        }).collect(Collectors.toList());

        populateProfileData(responseList);

        return responseList;
    }
}