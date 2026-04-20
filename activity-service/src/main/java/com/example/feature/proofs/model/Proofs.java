package com.example.feature.proofs.model;

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

    // [CẮT BỎ] Đổi từ Object Users sang ID nguyên thủy
    @Column(name = "student_id")
    private Long studentId;

    // [GIỮ NGUYÊN] Vì bảng Activities nằm chung DB iact_activity
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

    // [CẮT BỎ] Đổi từ Object Users sang ID nguyên thủy
    @Column(name = "verified_user")
    private Long verifiedBy;

    @Column(name = "verified_time")
    private LocalDateTime verifiedTime;
}