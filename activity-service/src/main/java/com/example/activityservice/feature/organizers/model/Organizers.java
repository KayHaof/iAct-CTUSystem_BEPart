package com.example.activityservice.feature.organizers.model;

import com.example.activityservice.feature.activities.model.Activities;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "organizers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "representative_user")
    private Long representativeUser;

    @OneToMany(mappedBy = "organizer", fetch = FetchType.LAZY)
    private List<Activities> activities;
}
