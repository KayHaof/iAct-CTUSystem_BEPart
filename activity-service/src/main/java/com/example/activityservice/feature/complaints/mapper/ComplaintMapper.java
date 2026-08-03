package com.example.activityservice.feature.complaints.mapper;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.model.Complaints;
import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ComplaintMapper {
    protected static final String DEFAULT_DETAIL_RESPONSE = "Chờ đơn vị tổ chức hoạt động phản hồi!";
    protected static final int LEGACY_TEXT_MAX_LENGTH = 255;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registration", source = "registration")
    @Mapping(target = "detail", expression = "java(request.getDetail() != null ? request.getDetail().trim() : null)")
    @Mapping(target = "reason", expression = "java(truncateDetail(request.getDetail(), LEGACY_TEXT_MAX_LENGTH))")
    @Mapping(target = "evidenceUrl", expression = "java(request.getEvidenceUrl() == null || request.getEvidenceUrl().isBlank() ? null : request.getEvidenceUrl().trim())")
    @Mapping(target = "detailResponse", expression = "java(DEFAULT_DETAIL_RESPONSE)")
    @Mapping(target = "status", expression = "java(0)")
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "response", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Complaints toNewEntity(ComplaintRequest request, Registrations registration);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registration", ignore = true)
    @Mapping(target = "detail", expression = "java(request.getDetail() != null ? request.getDetail().trim() : null)")
    @Mapping(target = "reason", expression = "java(truncateDetail(request.getDetail(), LEGACY_TEXT_MAX_LENGTH))")
    @Mapping(target = "evidenceUrl", expression = "java(request.getEvidenceUrl() == null || request.getEvidenceUrl().isBlank() ? null : request.getEvidenceUrl().trim())")
    @Mapping(target = "detailResponse", ignore = true)
    @Mapping(target = "status", expression = "java(0)")
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "response", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "resolvedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void updateEntityFromRequest(ComplaintRequest request, @MappingTarget Complaints complaint);

    @Mapping(target = "registrationId", source = "registration.id")
    @Mapping(target = "activityId", source = "activity.id")
    @Mapping(target = "activityTitle", source = "activity.title")
    @Mapping(target = "semesterId", source = "semester.id")
    @Mapping(target = "semesterName", source = "semester.name")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentCode", source = "student.studentCode")
    @Mapping(target = "studentName", source = "student.fullName")
    @Mapping(target = "statusLabel", expression = "java(resolveStatusLabel(complaint.getStatus()))")
    public abstract ComplaintResponse toResponse(Complaints complaint);

    @Mapping(target = "registrationId", source = "registration.id")
    @Mapping(target = "activityId", source = "registration.activity.id")
    @Mapping(target = "activityTitle", source = "registration.activity.title")
    @Mapping(target = "location", source = "registration.activity.location")
    @Mapping(target = "startDate", source = "registration.activity.startDate")
    @Mapping(target = "endDate", source = "registration.activity.endDate")
    @Mapping(target = "checkinTime", source = "registration.attendance.checkinTime")
    @Mapping(target = "checkoutTime", source = "registration.attendance.checkoutTime")
    @Mapping(target = "complaint", expression = "java(toResponse(complaint))")
    public abstract ComplaintEligibleActivityResponse toEligibleResponse(
            Registrations registration,
            Complaints complaint);

    public void syncRegistrationContext(Complaints complaint, Registrations registration) {
        if (complaint == null || registration == null) {
            return;
        }

        Activities activity = registration.getActivity();
        Users student = registration.getStudent();

        complaint.setRegistration(registration);
        complaint.setActivity(activity);
        complaint.setSemester(activity != null ? activity.getSemester() : null);
        complaint.setStudent(student);
    }

    @AfterMapping
    protected void syncNewEntityContext(Registrations registration, @MappingTarget Complaints complaint) {
        syncRegistrationContext(complaint, registration);
        applyDefaultDetailResponse(complaint);
    }

    @AfterMapping
    protected void applyDefaultDetailResponse(@MappingTarget Complaints complaint) {
        if (complaint.getDetailResponse() == null || complaint.getDetailResponse().isBlank()) {
            complaint.setDetailResponse(DEFAULT_DETAIL_RESPONSE);
        }
    }

    @AfterMapping
    protected void fillResponseFallbacks(Complaints complaint, @MappingTarget ComplaintResponse response) {
        Registrations registration = complaint.getRegistration();
        Activities activity = complaint.getActivity() != null
                ? complaint.getActivity()
                : (registration != null ? registration.getActivity() : null);
        Semesters semester = complaint.getSemester() != null
                ? complaint.getSemester()
                : (activity != null ? activity.getSemester() : null);
        Users student = complaint.getStudent() != null
                ? complaint.getStudent()
                : (registration != null ? registration.getStudent() : null);

        if (response.getRegistrationId() == null && registration != null) {
            response.setRegistrationId(registration.getId());
        }
        if (response.getActivityId() == null && activity != null) {
            response.setActivityId(activity.getId());
            response.setActivityTitle(activity.getTitle());
        }
        if (response.getSemesterId() == null && semester != null) {
            response.setSemesterId(semester.getId());
            response.setSemesterName(semester.getName());
        }
        if (response.getStudentId() == null && student != null) {
            response.setStudentId(student.getId());
            response.setStudentCode(student.getStudentCode());
            response.setStudentName(student.getFullName());
        }
    }

    protected String truncateDetail(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() <= maxLength) {
            return trimmedValue;
        }
        return trimmedValue.substring(0, maxLength);
    }

    protected String resolveStatusLabel(Integer status) {
        if (status == null || status == 0) {
            return "Đang chờ xử lý";
        }
        if (status == 1) {
            return "Đã duyệt";
        }
        if (status == 2) {
            return "Từ chối";
        }
        return "Không xác định";
    }
}
