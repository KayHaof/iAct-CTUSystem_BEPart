package com.example.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "organizers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizers {

    @Id
    private Long userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private com.example.feature.users.model.Users user;

    private String name;

//    @ManyToOne
//    @JoinColumn(name = "department_id")
//    private Departments department;
//
//    @ManyToOne
//    @JoinColumn(name = "representative_user")
//    private Users representative;
}

