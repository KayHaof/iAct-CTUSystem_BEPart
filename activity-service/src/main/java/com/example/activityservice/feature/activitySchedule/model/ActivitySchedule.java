package com.example.activityservice.feature.activitySchedule.model;

import com.example.activityservice.feature.activities.model.Activities;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_schedules")
public class ActivitySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activities activity;

    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    private String location;

    public ActivitySchedule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Activities getActivity() { return activity; }
    public void setActivity(Activities activity) { this.activity = activity; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
