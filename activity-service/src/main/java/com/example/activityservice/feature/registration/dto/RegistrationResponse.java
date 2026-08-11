package com.example.activityservice.feature.registration.dto;

import com.example.activityservice.feature.attendances.dto.AttendanceResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RegistrationResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String avatarUrl;
    private String studentCode;
    private Long classId;
    private String classCode;
    private String className;
    private String academicYear;
    private Long activityId;
    private String activityTitle;
    private LocalDateTime registeredAt;
    private Integer status; // 0=registered, 1=attended, 2=cancelled, 3=absent
    private String cancelReason;
    private String absenceReason;
    private Boolean absenceReviewed;
    private Long absenceReviewedBy;
    private LocalDateTime absenceReviewedAt;
    private String absenceReviewNote;

    private LocalDateTime attendedAt;
    private LocalDateTime checkoutAt;
    private Boolean isAttended;
    private String attendanceStatus;
    private String participationStatus;
    private Boolean canSubmitProof;
    private String nextAction;
    private Integer faceVerificationAttemptCount;
    private Integer faceVerificationMaxAttempts;
    private Integer faceVerificationRemainingAttempts;
    private Boolean faceVerificationExhausted;
    private Boolean canSubmitComplaint;

    private List<Long> scheduleIds;
    private List<AttendanceResponse> attendanceSessions;
    private Integer registeredSessionCount;
    private Integer faceVerifiedSessionCount;
    private Integer absentSessionCount;
    private Integer point;

    private Integer proofStatus;
}
