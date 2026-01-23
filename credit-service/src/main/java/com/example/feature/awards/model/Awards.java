package com.example.feature.awards.model;

import com.example.feature.award_criteria.model.Award_Criterias;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "awards")
@Data
public class Awards {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer type; // 1=danh_hieu, 2=hoc_bong, 3=khen_thuong
    private String description;

    @Lob
    private String requirements;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "award")
    private List<Award_Criterias> criteriaList;
}
