package com.example.activityservice.feature.registration.model;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "registrations")
public class Registrations {

    public static final int STATUS_REGISTERED = 0;
    public static final int STATUS_ATTENDED = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_ABSENT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Users student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activities activity;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    private Integer status = STATUS_REGISTERED;

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "absence_reason", columnDefinition = "TEXT")
    private String absenceReason;

    @Column(name = "absence_reviewed", nullable = false)
    private Boolean absenceReviewed = false;

    @Column(name = "absence_reviewed_by")
    private Long absenceReviewedBy;

    @Column(name = "absence_reviewed_at")
    private LocalDateTime absenceReviewedAt;

    @Column(name = "absence_review_note", columnDefinition = "TEXT")
    private String absenceReviewNote;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attendances> attendances = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "registration_schedules",
            joinColumns = @JoinColumn(name = "registration_id"),
            inverseJoinColumns = @JoinColumn(name = "schedule_id")
    )
    private List<ActivitySchedule> registeredSchedules = new ArrayList<>();

    public Registrations() {}

    @Transient
    public Attendances getAttendance() {
        if (attendances == null || attendances.isEmpty()) {
            return null;
        }
        return attendances.stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.getCheckinTime();
                    LocalDateTime rightTime = right.getCheckinTime();
                    if (leftTime == null && rightTime == null) {
                        return Long.compare(
                                left.getId() != null ? left.getId() : Long.MAX_VALUE,
                                right.getId() != null ? right.getId() : Long.MAX_VALUE);
                    }
                    if (leftTime == null) {
                        return 1;
                    }
                    if (rightTime == null) {
                        return -1;
                    }
                    return leftTime.compareTo(rightTime);
                })
                .findFirst()
                .orElse(null);
    }

}
