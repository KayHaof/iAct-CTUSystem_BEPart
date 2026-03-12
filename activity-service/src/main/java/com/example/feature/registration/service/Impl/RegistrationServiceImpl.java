package com.example.feature.registration.service.Impl;

import com.example.common.entity.Users;
import com.example.common.repository.LocalUserRepository;
import com.example.dto.PageDTO;
import com.example.exception.AppException;
import com.example.exception.ErrorCode;
import com.example.feature.activities.model.Activities;
import com.example.feature.activities.repository.ActivityRepository;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.activitySchedule.repository.ActivityScheduleRepository;
import com.example.feature.registration.dto.RegistrationRequest;
import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.mapper.RegistrationMapper;
import com.example.feature.registration.model.Registrations;
import com.example.feature.registration.repository.RegistrationRepository;
import com.example.feature.registration.service.RegistrationService;
import com.example.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityScheduleRepository scheduleRepository;
    private final LocalUserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final ExcelExportService excelExportService;

    // --- Lấy sinh viên đang đăng nhập ---
    private Users getCurrentStudent() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // ---  Xử lý bộ lọc tìm kiếm (Khắc phục lỗi Duplicated code) ---
    private Specification<Registrations> buildFilterSpecification(Long activityId, String keyword, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Bắt buộc phải thuộc Activity này
            predicates.add(cb.equal(root.get("activity").get("id"), activityId));

            // Lọc theo trạng thái
            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                try {
                    int statusCode = Integer.parseInt(status);
                    predicates.add(cb.equal(root.get("status"), statusCode));
                } catch (NumberFormatException ignored) {}
            }

            // Lọc theo keyword (Tên hoặc MSSV)
            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("student").get("fullName")), likeKeyword);
                Predicate codeMatch = cb.like(cb.lower(root.get("student").get("studentCode")), likeKeyword);
                predicates.add(cb.or(nameMatch, codeMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
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

        if (activity.getStatus() != 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này hiện chưa mở đăng ký.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getRegistrationStart()) || now.isAfter(activity.getRegistrationEnd())) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Rất tiếc, đã hết hoặc chưa tới thời hạn đăng ký.");
        }

        Registrations existingReg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activity.getId())
                .orElse(null);

        if (existingReg != null && (existingReg.getStatus() == 0 || existingReg.getStatus() == 1)) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã đăng ký hoạt động này rồi nha!");
        }

        long currentCount = registrationRepository.countByActivityIdAndStatusNot(activity.getId(), 2);
        if (currentCount >= activity.getMaxParticipants()) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Hoạt động này đã full chỗ mất rồi!");
        }

        List<ActivitySchedule> selectedSchedules = new ArrayList<>();
        if (request.getScheduleIds() != null && !request.getScheduleIds().isEmpty()) {
            selectedSchedules = scheduleRepository.findAllById(request.getScheduleIds());

            boolean allMatch = selectedSchedules.stream()
                    .allMatch(s -> s.getActivity().getId().equals(activity.getId()));
            if (!allMatch || selectedSchedules.size() != request.getScheduleIds().size()) {
                throw new AppException(ErrorCode.INVALID_ACTION, "Danh sách buổi đăng ký không hợp lệ!");
            }
        }

        Registrations regToSave;
        if (existingReg != null && existingReg.getStatus() == 2) {
            regToSave = existingReg;
            regToSave.setStatus(0);
            regToSave.setCancelReason(null);
            regToSave.setRegisteredAt(LocalDateTime.now());
            if (regToSave.getRegisteredSchedules() != null) {
                regToSave.getRegisteredSchedules().clear();
            }
        } else {
            regToSave = new Registrations();
            regToSave.setStudent(student);
            regToSave.setActivity(activity);
            regToSave.setRegisteredAt(LocalDateTime.now());
            regToSave.setStatus(0);
            regToSave.setRegisteredSchedules(new ArrayList<>());
        }

        if (!selectedSchedules.isEmpty()) {
            if (regToSave.getRegisteredSchedules() == null) {
                regToSave.setRegisteredSchedules(new ArrayList<>());
            }
            regToSave.getRegisteredSchedules().addAll(selectedSchedules);
        }

        return registrationMapper.toResponse(registrationRepository.save(regToSave));
    }

    @Override
    @Transactional
    public RegistrationResponse cancelByActivityId(Long activityId, String reason) {
        Users student = getCurrentStudent();
        Registrations reg = registrationRepository.findByStudentIdAndActivityId(student.getId(), activityId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_EXISTED, "Ní chưa đăng ký hoạt động này nên không hủy được!"));
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
        if (reg.getStatus() == 1) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Không được hủy khi đã điểm danh!");
        }
        if (reg.getStatus() == 2) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Bạn đã hủy đăng ký trước đó rồi!");
        }

        reg.setStatus(2);
        reg.setCancelReason(reason);

        if (reg.getRegisteredSchedules() != null) {
            reg.getRegisteredSchedules().clear();
        }

        return registrationMapper.toResponse(registrationRepository.save(reg));
    }

    @Override
    public PageDTO<RegistrationResponse> getParticipants(Long activityId, String keyword, String status, Pageable pageable) {
        Specification<Registrations> spec = buildFilterSpecification(activityId, keyword, status);

        Page<Registrations> pageData = registrationRepository.findAll(spec, pageable);

        List<RegistrationResponse> dtoList = pageData.getContent().stream()
                .map(registrationMapper::toResponse)
                .collect(Collectors.toList());

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

        reg.setStatus(status);
        if (status == 2) {
            reg.setCancelReason("Quản trị viên / Khoa hủy đăng ký");
            if (reg.getRegisteredSchedules() != null) {
                reg.getRegisteredSchedules().clear();
            }
        }

        return registrationMapper.toResponse(registrationRepository.save(reg));
    }

    @Override
    public void exportToExcel(Long activityId, String keyword, String status, OutputStream outputStream) {
        // 1. Logic lọc danh sách sinh viên
        Specification<Registrations> spec = buildFilterSpecification(activityId, keyword, status);
        List<Registrations> list = registrationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "registeredAt"));

        // 2. Mảng Header có thêm cột "Buổi đăng ký"
        String[] headers = {"STT", "MSSV", "Họ và Tên", "Thời gian ĐK", "Buổi đăng ký", "Trạng thái", "Lý do hủy"};

        java.util.concurrent.atomic.AtomicInteger stt = new java.util.concurrent.atomic.AtomicInteger(1);

        // Formatter để lấy mỗi Giờ:Phút (VD: 08:30)
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        try {
            excelExportService.export(
                    "Danh_sach_SV",
                    headers,
                    list,
                    (reg) -> {
                        String statusStr = reg.getStatus() == 0 ? "Đã đăng ký" : (reg.getStatus() == 1 ? "Đã tham gia" : "Đã hủy");

                        String schedulesStr = "";
                        if (reg.getRegisteredSchedules() != null && !reg.getRegisteredSchedules().isEmpty()) {
                            schedulesStr = reg.getRegisteredSchedules().stream()
                                    .map(schedule -> {
                                        String title = schedule.getTitle() != null ? schedule.getTitle() : "Ca " + schedule.getId();
                                        // Gắn thêm giờ bắt đầu - kết thúc (VD: Buổi 1 (08:00 - 11:30))
                                        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
                                            return title + " (" + schedule.getStartTime().format(timeFormatter) + " - " + schedule.getEndTime().format(timeFormatter) + ")";
                                        }
                                        return title;
                                    })
                                    .collect(Collectors.joining(",\n")); // Dùng dấu phẩy và xuống dòng để Excel dễ nhìn nếu đk nhiều buổi
                        }

                        return new Object[]{
                                stt.getAndIncrement(),
                                reg.getStudent() != null ? reg.getStudent().getStudentCode() : "",
                                reg.getStudent() != null ? reg.getStudent().getFullName() : "",
                                reg.getRegisteredAt(),
                                schedulesStr,
                                statusStr,
                                reg.getCancelReason() != null ? reg.getCancelReason() : ""
                        };
                    },
                    outputStream
            );
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_ACTION, "Lỗi khi xuất file Excel!");
        }
    }
}