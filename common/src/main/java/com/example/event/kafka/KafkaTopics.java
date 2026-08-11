package com.example.event.kafka;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String LEGACY_IDENTITY_USER_CREATED = "iact.identity.user.created";
    public static final String LEGACY_IDENTITY_USER_SNAPSHOT = "iact.identity.user.snapshot";
    public static final String LEGACY_NOTIFICATION_CREATED = "iact.notification.created";
    public static final String LEGACY_ACTIVITY_DELETED = "iact.activity.deleted";

    public static final String USER_CREATED = "iact.user.user.created";
    public static final String USER_UPDATED = "iact.user.user.updated";
    public static final String USER_DEACTIVATED = "iact.user.user.deactivated";
    public static final String USER_DELETED = "iact.user.user.deleted";
    public static final String USER_ROLE_CHANGED = "iact.user.user.role-changed";
    public static final String USER_SNAPSHOT = "iact.user.user.snapshot";
    public static final String PROFILE_CREATED = "iact.user.profile.created";
    public static final String PROFILE_UPDATED = "iact.user.profile.updated";
    public static final String STUDENT_FACE_EMBEDDING_SNAPSHOT = "iact.user.student-face-embedding.snapshot";
    public static final String PREFERENCE_CREATED = "iact.user.preference.created";
    public static final String PREFERENCE_UPDATED = "iact.user.preference.updated";
    public static final String PREFERENCE_DELETED = "iact.user.preference.deleted";

    public static final String ACTIVITY_CREATED = "iact.activity.activity.created";
    public static final String ACTIVITY_UPDATED = "iact.activity.activity.updated";
    public static final String ACTIVITY_DELETED = "iact.activity.activity.deleted";
    public static final String ACTIVITY_SUBMITTED = "iact.activity.activity.submitted";
    public static final String ACTIVITY_APPROVED = "iact.activity.activity.approved";
    public static final String ACTIVITY_REJECTED = "iact.activity.activity.rejected";
    public static final String ACTIVITY_CANCELLED = "iact.activity.activity.cancelled";
    public static final String ACTIVITY_DRAFT_EXPIRED = "iact.activity.activity.draft-expired";
    public static final String REGISTRATION_CREATED = "iact.activity.registration.created";
    public static final String REGISTRATION_CANCELLED = "iact.activity.registration.cancelled";
    public static final String ATTENDANCE_CHECKED_IN = "iact.activity.attendance.checked-in";
    public static final String PROOF_SUBMITTED = "iact.activity.proof.submitted";
    public static final String PROOF_APPROVED = "iact.activity.proof.approved";
    public static final String PROOF_REJECTED = "iact.activity.proof.rejected";
    public static final String CERTIFICATE_SUBMISSION_SUBMITTED = "iact.activity.certificate-submission.submitted";
    public static final String CERTIFICATE_SUBMISSION_APPROVED = "iact.activity.certificate-submission.approved";
    public static final String CERTIFICATE_SUBMISSION_REJECTED = "iact.activity.certificate-submission.rejected";
    public static final String POINT_AWARDED = "iact.activity.point.awarded";
    public static final String POINT_RECALCULATED = "iact.activity.point.recalculated";
    public static final String POINT_REVOKED = "iact.activity.point.revoked";

    public static final String NOTIFICATION_CREATED = "iact.notification.notification.created";
    public static final String NOTIFICATION_DISPATCHED = "iact.notification.notification.dispatched";
    public static final String NOTIFICATION_READ = "iact.notification.notification.read";
    public static final String NOTIFICATION_DELETED = "iact.notification.notification.deleted";
    public static final String NOTIFICATION_CLEANUP_REQUESTED = "iact.notification.notification.cleanup-requested";
    public static final String NOTIFICATION_BROADCAST_REQUESTED = "iact.notification.notification.broadcast-requested";
    public static final String NOTIFICATION_ACTIVITY_SESSION_ACTION_REMINDER_REQUESTED =
            "iact.notification.notification.activity-session-action-reminder-requested";
    public static final String NOTIFICATION_ABSENCE_VIOLATION_PROCESSED_REQUESTED =
            "iact.notification.notification.absence-violation-processed-requested";
    public static final String NOTIFICATION_URGENT_REQUESTED = "iact.notification.notification.urgent-requested";
    public static final String NOTIFICATION_DELIVERY_FAILED = "iact.notification.delivery.failed";
    public static final String SYSTEM_DEAD_LETTER = "iact.system.dead-letter";
}
