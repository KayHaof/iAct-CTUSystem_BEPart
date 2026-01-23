package com.example.common;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.example.feature.users.model.Users user;

    private String title;
    private String message;
    private Integer type; // 1=new, 2=update, 3=alert, 4=proof, 5=reply

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "activity_id")
//    private Activities activity;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

