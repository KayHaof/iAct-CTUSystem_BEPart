package com.example.activityservice.feature.complaints.mapper;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.complaints.dto.ComplaintEligibleActivityResponse;
import com.example.activityservice.feature.complaints.dto.ComplaintRequest;
import com.example.activityservice.feature.complaints.dto.ComplaintResponse;
import com.example.activityservice.feature.complaints.model.Complaints;
import com.example.activityservice.feature.registration.model.Registrations;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {
    public Complaints toNewEntity(ComplaintRequest request, Registrations registration) {
        Complaints complaint = new Complaints();
        complaint.setRegistration(registration);
        updateEntityFromRequest(request, complaint);
        complaint.setStatus(0);
        return complaint;
    }

    public void updateEntityFromRequest(ComplaintRequest request, Complaints complaint) {
        complaint.setDetail(request.getDetail().trim());
        complaint.setEvidenceUrl(normalize(request.getEvidenceUrl()));
    }

    public ComplaintResponse toResponse(Complaints complaint) {
        if (complaint == null) {
            return null;
        }

        Registrations registration = complaint.getRegistration();
        Activities activity = registration != null ? registration.getActivity() : null;

        ComplaintResponse response = new ComplaintResponse();
        response.setId(complaint.getId());
        response.setRegistrationId(registration != null ? registration.getId() : null);
        response.setActivityId(activity != null ? activity.getId() : null);
        response.setActivityTitle(activity != null ? activity.getTitle() : null);
        response.setDetail(complaint.getDetail());
        response.setEvidenceUrl(complaint.getEvidenceUrl());
        response.setResponse(complaint.getResponse());
        response.setStatus(complaint.getStatus());
        response.setStatusLabel(resolveStatusLabel(complaint.getStatus()));
        response.setResolvedAt(complaint.getResolvedAt());
        response.setCreatedAt(complaint.getCreatedAt());
        response.setUpdatedAt(complaint.getUpdatedAt());
        return response;
    }

    public ComplaintEligibleActivityResponse toEligibleResponse(
            Registrations registration,
            Complaints complaint) {
        Activities activity = registration.getActivity();
        Attendances attendance = registration.getAttendance();

        ComplaintEligibleActivityResponse response = new ComplaintEligibleActivityResponse();
        response.setRegistrationId(registration.getId());
        response.setActivityId(activity != null ? activity.getId() : null);
        response.setActivityTitle(activity != null ? activity.getTitle() : null);
        response.setLocation(activity != null ? activity.getLocation() : null);
        response.setStartDate(activity != null ? activity.getStartDate() : null);
        response.setEndDate(activity != null ? activity.getEndDate() : null);
        response.setCheckinTime(attendance != null ? attendance.getCheckinTime() : null);
        response.setCheckoutTime(attendance != null ? attendance.getCheckoutTime() : null);
        response.setComplaint(toResponse(complaint));
        return response;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String resolveStatusLabel(Integer status) {
        if (status == null || status == 0) {
            return "Đang chờ xử lý";
        }
        if (status == 1) {
            return "Đã phản hồi";
        }
        if (status == 2) {
            return "Từ chối";
        }
        return "Không xác định";
    }
}
