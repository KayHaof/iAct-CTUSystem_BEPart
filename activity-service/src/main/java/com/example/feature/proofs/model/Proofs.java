package com.example.feature.proofs.model;

import com.example.common.Users;
import com.example.feature.activities.model.Activities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proofs")
@Data
public class Proofs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Users student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activities activity;

    @Column(name = "image_url")
    private String imageUrl;

    private Integer status; // 0=pending, 1=approved, 2=rejected

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_user")
    private Users verifier;
}

