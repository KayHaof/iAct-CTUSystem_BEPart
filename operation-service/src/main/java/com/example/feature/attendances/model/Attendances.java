package com.example.feature.attendances.model;

import com.example.common.Registrations;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Data
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

    private Integer method; // 1=qr, 2=manual, 3=face_id
}

