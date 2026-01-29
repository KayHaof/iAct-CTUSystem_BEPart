package com.example.feature.activities.model;

import com.example.common.entity.Benefits;
import com.example.common.entity.Semesters;
import com.example.common.entity.Users;
import com.example.feature.organizers.model.Organizers;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Activities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "cover_image")
    private String coverImage;
    private String thumbnail;

    @Column(name = "source_link")
    private String sourceLink;

    @Column(name = "is_external")
    private Boolean isExternal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private Organizers organizer;

    @Column(name = "qr_code_token", unique = true)
    private String qrCodeToken;

    private Integer status; // 0 = pending. 1 = active, 2 = reject

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Users createdBy;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Benefits> benefits;
}