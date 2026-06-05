package com.example.activityservice.feature.complaints.model;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Users student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activities activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    private String reason;

    @Lob
    private String detail;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    private Integer status;

    @Column(name = "detail_response")
    private String detailResponse;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Complaints() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Users getStudent() { return student; }
    public void setStudent(Users student) { this.student = student; }
    public Activities getActivity() { return activity; }
    public void setActivity(Activities activity) { this.activity = activity; }
    public Semesters getSemester() { return semester; }
    public void setSemester(Semesters semester) { this.semester = semester; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getDetailResponse() { return detailResponse; }
    public void setDetailResponse(String detailResponse) { this.detailResponse = detailResponse; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
