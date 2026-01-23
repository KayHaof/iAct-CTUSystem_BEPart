package com.example.common;

import com.example.feature.users.model.Users;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Table(name = "classes")
@Data
@RequiredArgsConstructor
public class Clazzes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", unique = true)
    private String classCode;

    private String name;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "major_id")
//    private Majors major;

    @Column(name = "academic_year")
    private Integer academicYear;

    @OneToMany(mappedBy = "clazz")
    private List<Users> students;
}
