package com.example.feature.organizers.model;

import com.example.feature.activities.model.Activities;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "organizers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organizers {

    @Id
    @Column(name = "user_id")
    private Long id;

    private String name;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "representative_user")
    private Long representativeUser;

    @OneToMany(mappedBy = "organizer", fetch = FetchType.LAZY)
    private List<Activities> activities;
}