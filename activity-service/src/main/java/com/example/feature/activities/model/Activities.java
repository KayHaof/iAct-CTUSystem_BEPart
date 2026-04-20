package com.example.feature.activities.model;

import com.example.feature.activitySchedule.model.ActivitySchedule;
import com.example.feature.organizers.model.Organizers;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Activities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "cover_image")
    private String coverImage;
    private String thumbnail;

    @Column(name = "source_link")
    private String sourceLink;

    @Column(name = "is_external")
    private Boolean isExternal;

    @Column(name = "is_faculty")
    private Boolean isFaculty;

    @Column(name = "qr_code_token", unique = true)
    private String qrCodeToken;

    private Integer status; // 0 = pending. 1 = active, 2 = reject, 3 = cancel, 4 = draft

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "created_by_username")
    private String createdByUsername;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private Organizers organizer;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivitySchedule> schedules = new ArrayList<>();

    // Giao tiếp với các DB khác
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "semester_id")
    private Long semesterId;

    public void addSchedule(ActivitySchedule schedule) {
        schedules.add(schedule);
        schedule.setActivity(this);
    }

    public void removeSchedule(ActivitySchedule schedule) {
        schedules.remove(schedule);
        schedule.setActivity(null);
    }
}