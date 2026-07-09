package com.example.activityservice.feature.complaints.model;

import com.example.activityservice.feature.registration.model.Registrations;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @JoinColumn(name = "registration_id", nullable = false)
    private Registrations registration;

    @Lob
    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(name = "evidence_url")
    private String evidenceUrl;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    private Integer status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Users resolvedBy;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Complaints() {}

}
