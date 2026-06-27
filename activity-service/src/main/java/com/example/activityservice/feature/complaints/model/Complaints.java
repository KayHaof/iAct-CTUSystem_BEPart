package com.example.activityservice.feature.complaints.model;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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

}
