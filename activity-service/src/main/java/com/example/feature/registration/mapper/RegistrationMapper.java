package com.example.feature.registration.mapper;

import com.example.common.entity.Benefits;
import com.example.common.entity.Users;
import com.example.feature.activities.model.Activities;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.registration.dto.RegistrationResponse;
import com.example.feature.registration.model.Registrations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {Collectors.class, ArrayList.class})
public interface RegistrationMapper {

    // --- 1. Map dữ liệu trả về ---
    @Mapping(target = "studentId", source = "entity.student.id")
    @Mapping(target = "studentName", source = "entity.student.fullName")
    @Mapping(target = "avatarUrl", source = "entity.student.avatarUrl")
    @Mapping(target = "studentCode", source = "entity.student.studentCode")
    @Mapping(target = "activityId", source = "entity.activity.id")
    @Mapping(target = "activityTitle", source = "entity.activity.title")
    @Mapping(target = "isAttended", expression = "java(entity.getAttendance() != null)")
    @Mapping(target = "attendedAt", source = "entity.attendance.checkinTime")
    @Mapping(target = "scheduleIds", expression = "java( entity.getRegisteredSchedules() != null ? entity.getRegisteredSchedules().stream().map(s -> s.getId()).collect(Collectors.toList()) : new ArrayList<Long>() )")
    @Mapping(target = "proofStatus", source = "proofStatus")
    @Mapping(target = "point", expression = "java( calculateTotalPoints(entity.getActivity().getBenefits()) )")
    RegistrationResponse toResponseWithProof(Registrations entity, Integer proofStatus);

    default RegistrationResponse toResponse(Registrations entity) {
        return toResponseWithProof(entity, 0);
    }

    // --- 2. Map Tạo mới đơn đăng ký (Insert) ---
    default Registrations toNewEntity(Users student, Activities activity, List<ActivitySchedule> schedules) {
        Registrations reg = new Registrations();
        reg.setStudent(student);
        reg.setActivity(activity);
        reg.setRegisteredSchedules(schedules != null ? schedules : new ArrayList<>());
        reg.setStatus(0);
        reg.setRegisteredAt(LocalDateTime.now());
        return reg;
    }

    // --- 3. Map Cập nhật đăng ký lại từ đơn đã hủy (Update) ---
    default void reRegisterEntity(Registrations entity, List<ActivitySchedule> schedules) {
        entity.setStatus(0);
        entity.setCancelReason(null);
        entity.setRegisteredAt(LocalDateTime.now());
        entity.setRegisteredSchedules(schedules != null ? schedules : new ArrayList<>());
    }

    // --- 4. Map Hủy đăng ký ---
    default void cancelEntity(Registrations entity, String reason) {
        entity.setStatus(2);
        entity.setCancelReason(reason);
        if (entity.getRegisteredSchedules() != null) {
            entity.getRegisteredSchedules().clear();
        }
    }

    default Integer calculateTotalPoints(List<Benefits> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return 0;
        }
        return benefits.stream()
                .filter(b -> b.getPoint() != null)
                .mapToInt(Benefits::getPoint)
                .sum();
    }
}