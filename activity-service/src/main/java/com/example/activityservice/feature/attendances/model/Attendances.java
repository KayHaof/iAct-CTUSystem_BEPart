package com.example.activityservice.feature.attendances.model;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.registration.model.Registrations;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "attendances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_registration_schedule",
                columnNames = {"registration_id", "schedule_id"}
        )
)
public class Attendances {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CHECKED_IN = 1;
    public static final int STATUS_CHECKED_OUT = 2;
    public static final int STATUS_FACE_VERIFIED = 3;
    public static final int STATUS_ABSENT = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private Registrations registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ActivitySchedule schedule;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer method;
    private Integer status = STATUS_PENDING;

    public Attendances() {}

    public static AttendancesBuilder builder() { return new AttendancesBuilder(); }
}
