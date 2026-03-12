package com.example.feature.registration.model;

import com.example.common.entity.Users;
import com.example.feature.activities.model.Activities;
import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.attendances.model.Attendances;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations")
@Data
public class Registrations {
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

    private Integer status; // 0=registered, 1=attended, 2=cancelled

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL)
    private Attendances attendance;

    @ManyToMany
    @JoinTable(
            name = "registration_schedules",
            joinColumns = @JoinColumn(name = "registration_id"),
            inverseJoinColumns = @JoinColumn(name = "schedule_id")
    )
    private List<ActivitySchedule> registeredSchedules = new ArrayList<>();
}