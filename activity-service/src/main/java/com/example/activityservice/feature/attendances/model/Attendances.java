package com.example.activityservice.feature.attendances.model;

import com.example.activityservice.feature.registration.model.Registrations;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Registrations getRegistration() { return registration; }
    public void setRegistration(Registrations registration) { this.registration = registration; }
    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }
    public LocalDateTime getCheckoutTime() { return checkoutTime; }
    public void setCheckoutTime(LocalDateTime checkoutTime) { this.checkoutTime = checkoutTime; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Integer getMethod() { return method; }
    public void setMethod(Integer method) { this.method = method; }

    public static AttendancesBuilder builder() { return new AttendancesBuilder(); }
}
