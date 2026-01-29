package com.example.feature.organizers.model;

import com.example.common.entity.Departments;
import com.example.common.entity.Users;
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

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private Users user;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Departments department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "representative_user")
    private Users representative;

    @OneToMany(mappedBy = "organizer", fetch = FetchType.LAZY)
    private List<Activities> activities;
}