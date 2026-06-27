package com.example.activityservice.feature.awards.model;

import com.example.activityservice.feature.award_criteria.model.Award_Criterias;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "awards")
public class Awards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer type;
    private String description;

    @Lob
    private String requirements;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "award")
    private List<Award_Criterias> criteriaList;

    public Awards() {}

}
