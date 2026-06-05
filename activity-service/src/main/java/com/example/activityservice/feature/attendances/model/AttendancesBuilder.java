package com.example.activityservice.feature.attendances.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendancesBuilder {
    private final Attendances instance = new Attendances();

    public AttendancesBuilder id(Long id) { instance.setId(id); return this; }
    public AttendancesBuilder registration(com.example.activityservice.feature.registration.model.Registrations r) { instance.setRegistration(r); return this; }
    public AttendancesBuilder checkinTime(LocalDateTime t) { instance.setCheckinTime(t); return this; }
    public AttendancesBuilder checkoutTime(LocalDateTime t) { instance.setCheckoutTime(t); return this; }
    public AttendancesBuilder latitude(BigDecimal v) { instance.setLatitude(v); return this; }
    public AttendancesBuilder longitude(BigDecimal v) { instance.setLongitude(v); return this; }
    public AttendancesBuilder method(Integer m) { instance.setMethod(m); return this; }
    public Attendances build() { return instance; }
}
