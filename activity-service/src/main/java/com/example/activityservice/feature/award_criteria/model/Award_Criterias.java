package com.example.activityservice.feature.award_criteria.model;

import com.example.activityservice.feature.awards.model.Awards;
import com.example.activityservice.feature.categories.model.Categories;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "award_criteria")
public class Award_Criterias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id")
    private Awards award;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories category;

    @Column(name = "min_point_required")
    private Integer minPointRequired;

    public Award_Criterias() {}

}
