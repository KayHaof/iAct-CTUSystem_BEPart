package com.example.common;

import com.example.feature.users.model.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Activities {
    @Id
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
    private Integer maxParticipants;

//    @ManyToOne
//    @JoinColumn(name = "semester_id")
//    private Semesters semesters;
//
//    @ManyToOne
//    @JoinColumn(name = "organizer_id")
//    private Organizers organizers;
//
    @ManyToOne
    @JoinColumn(name = "created_by")
    private Users createdBy;

    private Boolean isExternal;
    private String qrCodeToken;
    private Integer status;
}


