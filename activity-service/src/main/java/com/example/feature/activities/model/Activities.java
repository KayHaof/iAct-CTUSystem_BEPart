package com.example.feature.activities.model;

import com.example.common.Benefits;
import com.example.common.Semesters;
import com.example.common.Users;
import com.example.feature.organizers.model.Organizers;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "activities")
@Data
public class Activities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;

    @Lob
    private String content;

    @Column(name = "registration_start")
    private LocalDateTime registrationStart;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private Organizers organizer;

    @Column(name = "qr_code_token", unique = true)
    private String qrCodeToken;

    private Integer status; // 0=draft, 1=published, 2=closed, 3=cancelled

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL)
    private List<Benefits> benefits;
}


