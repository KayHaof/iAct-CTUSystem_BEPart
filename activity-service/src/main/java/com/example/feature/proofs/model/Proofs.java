package com.example.feature.proofs.model;

import com.example.common.entity.Users;
import com.example.feature.activities.model.Activities;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "proofs")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(columnDefinition = "TEXT")
    private String description;

    // 0: Chờ duyệt, 1: Đã duyệt, 2: Bị từ chối
    private Integer status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Người kiểm duyệt (Admin/Cố vấn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_user")
    private Users verifiedUser;

    @Column(name = "verified_time")
    private LocalDateTime verifiedTime;
}

