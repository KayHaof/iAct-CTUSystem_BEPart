package com.example.common;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
@Data
public class Registrations {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Users student;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "activity_id")
//    private Activities activity;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    private Integer status; // 0=registered, 1=attended, 2=cancelled

//    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL)
//    private Attendances attendance;
}