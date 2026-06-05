package com.example.activityservice.feature.registration.model;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.attendances.model.Attendances;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations")
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

    private Integer status;

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

    public Registrations() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Users getStudent() { return student; }
    public void setStudent(Users student) { this.student = student; }
    public Activities getActivity() { return activity; }
    public void setActivity(Activities activity) { this.activity = activity; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public Attendances getAttendance() { return attendance; }
    public void setAttendance(Attendances attendance) { this.attendance = attendance; }
    public List<ActivitySchedule> getRegisteredSchedules() { return registeredSchedules; }
    public void setRegisteredSchedules(List<ActivitySchedule> registeredSchedules) { this.registeredSchedules = registeredSchedules; }
}
