package com.example.common;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
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
    private com.example.feature.semesters.model.Semesters semester;

    private String reason;

    @Lob
    private String detail;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    private Integer status; // 0=pending, 1=resolved, 2=rejected

    @Column(name = "detail_response")
    private String detailResponse;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}

