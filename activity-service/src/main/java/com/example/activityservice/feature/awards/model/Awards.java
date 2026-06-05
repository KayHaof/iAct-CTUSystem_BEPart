package com.example.activityservice.feature.awards.model;

import com.example.activityservice.feature.award_criteria.model.Award_Criterias;
import jakarta.persistence.*;

import java.util.List;

@Entity
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<Award_Criterias> getCriteriaList() { return criteriaList; }
    public void setCriteriaList(List<Award_Criterias> criteriaList) { this.criteriaList = criteriaList; }
}
