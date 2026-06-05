package com.example.activityservice.feature.award_criteria.model;

import com.example.activityservice.feature.awards.model.Awards;
import com.example.activityservice.feature.categories.model.Categories;
import jakarta.persistence.*;

@Entity
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Awards getAward() { return award; }
    public void setAward(Awards award) { this.award = award; }
    public Categories getCategory() { return category; }
    public void setCategory(Categories category) { this.category = category; }
    public Integer getMinPointRequired() { return minPointRequired; }
    public void setMinPointRequired(Integer minPointRequired) { this.minPointRequired = minPointRequired; }
}
