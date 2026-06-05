package com.example.activityservice.feature.activities.model;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.categories.model.Categories;
import com.example.activityservice.feature.organizers.model.Organizers;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories category;

    public Activities() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(LocalDateTime registrationStart) { this.registrationStart = registrationStart; }
    public LocalDateTime getRegistrationEnd() { return registrationEnd; }
    public void setRegistrationEnd(LocalDateTime registrationEnd) { this.registrationEnd = registrationEnd; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public String getSourceLink() { return sourceLink; }
    public void setSourceLink(String sourceLink) { this.sourceLink = sourceLink; }
    public Boolean getIsExternal() { return isExternal; }
    public void setIsExternal(Boolean isExternal) { this.isExternal = isExternal; }
    public Boolean getIsFaculty() { return isFaculty; }
    public void setIsFaculty(Boolean isFaculty) { this.isFaculty = isFaculty; }
    public String getQrCodeToken() { return qrCodeToken; }
    public void setQrCodeToken(String qrCodeToken) { this.qrCodeToken = qrCodeToken; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Organizers getOrganizer() { return organizer; }
    public void setOrganizer(Organizers organizer) { this.organizer = organizer; }
    public List<ActivitySchedule> getSchedules() { return schedules; }
    public void setSchedules(List<ActivitySchedule> schedules) { this.schedules = schedules; }
    public Users getCreatedBy() { return createdBy; }
    public void setCreatedBy(Users createdBy) { this.createdBy = createdBy; }
    public Users getHandledBy() { return handledBy; }
    public void setHandledBy(Users handledBy) { this.handledBy = handledBy; }
    public Semesters getSemester() { return semester; }
    public void setSemester(Semesters semester) { this.semester = semester; }
    public Categories getCategory() { return category; }
    public void setCategory(Categories category) { this.category = category; }

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
        public ActivitiesBuilder category(Categories category) { instance.setCategory(category); return this; }
        public Activities build() { return instance; }
    }
}
