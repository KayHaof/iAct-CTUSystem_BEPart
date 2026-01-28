package com.example.feature.departments.model;

import com.example.feature.major.model.Majors;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Thêm Builder để dễ khởi tạo object
public class Departments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    // JsonIgnore hoặc tách DTO để tránh vòng lặp vô tận khi convert JSON
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Majors> majors;
}