package com.example.event.kafka;

public final class KafkaEventTypes {
    private KafkaEventTypes() {
    }

    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DEACTIVATED = "USER_DEACTIVATED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String USER_SNAPSHOT = "USER_SNAPSHOT";
    public static final String PROFILE_CREATED = "PROFILE_CREATED";
    public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String PREFERENCE_CREATED = "PREFERENCE_CREATED";
    public static final String PREFERENCE_UPDATED = "PREFERENCE_UPDATED";
    public static final String PREFERENCE_DELETED = "PREFERENCE_DELETED";

    public static final String ACTIVITY_CREATED = "ACTIVITY_CREATED";
    public static final String ACTIVITY_UPDATED = "ACTIVITY_UPDATED";
    public static final String ACTIVITY_DELETED = "ACTIVITY_DELETED";
    public static final String ACTIVITY_SUBMITTED = "ACTIVITY_SUBMITTED";
    public static final String ACTIVITY_APPROVED = "ACTIVITY_APPROVED";
    public static final String ACTIVITY_REJECTED = "ACTIVITY_REJECTED";
    public static final String ACTIVITY_CANCELLED = "ACTIVITY_CANCELLED";
    public static final String ACTIVITY_DRAFT_EXPIRED = "ACTIVITY_DRAFT_EXPIRED";

    public static final String REGISTRATION_CREATED = "REGISTRATION_CREATED";
    public static final String REGISTRATION_CANCELLED = "REGISTRATION_CANCELLED";
    public static final String ATTENDANCE_CHECKED_IN = "ATTENDANCE_CHECKED_IN";
    public static final String PROOF_SUBMITTED = "PROOF_SUBMITTED";
    public static final String PROOF_APPROVED = "PROOF_APPROVED";
    public static final String PROOF_REJECTED = "PROOF_REJECTED";
    public static final String POINT_AWARDED = "POINT_AWARDED";
    public static final String POINT_RECALCULATED = "POINT_RECALCULATED";
    public static final String POINT_REVOKED = "POINT_REVOKED";

    public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";
    public static final String NOTIFICATION_DISPATCHED = "NOTIFICATION_DISPATCHED";
    public static final String NOTIFICATION_READ = "NOTIFICATION_READ";
    public static final String NOTIFICATION_DELETED = "NOTIFICATION_DELETED";
    public static final String NOTIFICATION_CLEANUP_REQUESTED = "NOTIFICATION_CLEANUP_REQUESTED";
    public static final String NOTIFICATION_BROADCAST_REQUESTED = "NOTIFICATION_BROADCAST_REQUESTED";
    public static final String NOTIFICATION_URGENT_REQUESTED = "NOTIFICATION_URGENT_REQUESTED";
    public static final String NOTIFICATION_DELIVERY_FAILED = "NOTIFICATION_DELIVERY_FAILED";
}

