package com.example.activityservice.feature.attendances.model;

import com.example.activityservice.feature.registration.model.Registrations;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "attendances")
public class Attendances {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private Registrations registration;

    @Column(name = "checkin_time")
    private LocalDateTime checkinTime;

    @Column(name = "checkout_time")
    private LocalDateTime checkoutTime;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer method;

    public Attendances() {}

    public static AttendancesBuilder builder() { return new AttendancesBuilder(); }
}
