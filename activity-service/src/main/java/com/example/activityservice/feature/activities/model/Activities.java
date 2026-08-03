package com.example.activityservice.feature.activities.model;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.organizers.model.Organizers;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "activities")
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

    @Column(name = "requires_admin_approval")
    private Boolean requiresAdminApproval = false;

    @Column(name = "qr_code_token", unique = true)
    private String qrCodeToken;

    private Integer status;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Users createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private Users handledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    public Activities() {}

    public void addSchedule(ActivitySchedule schedule) {
        schedules.add(schedule);
        schedule.setActivity(this);
    }

    public void removeSchedule(ActivitySchedule schedule) {
        schedules.remove(schedule);
        schedule.setActivity(null);
    }

    @Override
    public boolean equals(Object o) { return o != null && Objects.equals(getId(), ((Activities) o).getId()); }
    @Override
    public int hashCode() { return Objects.hashCode(getId()); }

    public static ActivitiesBuilder builder() { return new ActivitiesBuilder(); }

    public static class ActivitiesBuilder {
        private final Activities instance = new Activities();
        public ActivitiesBuilder id(Long id) { instance.setId(id); return this; }
        public ActivitiesBuilder title(String title) { instance.setTitle(title); return this; }
        public ActivitiesBuilder description(String description) { instance.setDescription(description); return this; }
        public ActivitiesBuilder content(String content) { instance.setContent(content); return this; }
        public ActivitiesBuilder registrationStart(LocalDateTime v) { instance.setRegistrationStart(v); return this; }
        public ActivitiesBuilder registrationEnd(LocalDateTime v) { instance.setRegistrationEnd(v); return this; }
        public ActivitiesBuilder startDate(LocalDateTime v) { instance.setStartDate(v); return this; }
        public ActivitiesBuilder endDate(LocalDateTime v) { instance.setEndDate(v); return this; }
        public ActivitiesBuilder location(String location) { instance.setLocation(location); return this; }
        public ActivitiesBuilder maxParticipants(Integer maxParticipants) { instance.setMaxParticipants(maxParticipants); return this; }
        public ActivitiesBuilder coverImage(String coverImage) { instance.setCoverImage(coverImage); return this; }
        public ActivitiesBuilder thumbnail(String thumbnail) { instance.setThumbnail(thumbnail); return this; }
        public ActivitiesBuilder sourceLink(String sourceLink) { instance.setSourceLink(sourceLink); return this; }
        public ActivitiesBuilder isExternal(Boolean isExternal) { instance.setIsExternal(isExternal); return this; }
        public ActivitiesBuilder isFaculty(Boolean isFaculty) { instance.setIsFaculty(isFaculty); return this; }
        public ActivitiesBuilder requiresAdminApproval(Boolean requiresAdminApproval) { instance.setRequiresAdminApproval(requiresAdminApproval); return this; }
        public ActivitiesBuilder qrCodeToken(String qrCodeToken) { instance.setQrCodeToken(qrCodeToken); return this; }
        public ActivitiesBuilder status(Integer status) { instance.setStatus(status); return this; }
        public ActivitiesBuilder departmentId(Long departmentId) { instance.setDepartmentId(departmentId); return this; }
        public ActivitiesBuilder reason(String reason) { instance.setReason(reason); return this; }
        public ActivitiesBuilder handledAt(LocalDateTime handledAt) { instance.setHandledAt(handledAt); return this; }
        public ActivitiesBuilder createdByUsername(String createdByUsername) { instance.setCreatedByUsername(createdByUsername); return this; }
        public ActivitiesBuilder updatedAt(LocalDateTime updatedAt) { instance.setUpdatedAt(updatedAt); return this; }
        public ActivitiesBuilder createdAt(LocalDateTime createdAt) { instance.setCreatedAt(createdAt); return this; }
        public ActivitiesBuilder organizer(Organizers organizer) { instance.setOrganizer(organizer); return this; }
        public ActivitiesBuilder schedules(List<ActivitySchedule> schedules) { instance.setSchedules(schedules); return this; }
        public ActivitiesBuilder createdBy(Users createdBy) { instance.setCreatedBy(createdBy); return this; }
        public ActivitiesBuilder handledBy(Users handledBy) { instance.setHandledBy(handledBy); return this; }
        public ActivitiesBuilder semester(Semesters semester) { instance.setSemester(semester); return this; }
        public Activities build() { return instance; }
    }
}
