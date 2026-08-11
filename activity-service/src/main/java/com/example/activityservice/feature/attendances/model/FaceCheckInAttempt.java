package com.example.activityservice.feature.attendances.model;

import com.example.activityservice.feature.activitySchedule.model.ActivitySchedule;
import com.example.activityservice.feature.registration.model.Registrations;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "face_checkin_attempts")
public class FaceCheckInAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registrations registration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ActivitySchedule schedule;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "allow_retry", nullable = false)
    private Boolean allowRetry;

    @Column(name = "decision", length = 50)
    private String decision;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "threshold", precision = 10, scale = 6)
    private BigDecimal threshold;

    @Column(name = "distance", precision = 10, scale = 6)
    private BigDecimal distance;

    @Column(name = "similarity", precision = 10, scale = 6)
    private BigDecimal similarity;

    @Column(name = "reference_embedding_version")
    private Integer referenceEmbeddingVersion;

    @Column(name = "reference_model_name", length = 100)
    private String referenceModelName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
