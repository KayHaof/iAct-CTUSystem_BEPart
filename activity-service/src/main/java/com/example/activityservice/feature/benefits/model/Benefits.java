package com.example.activityservice.feature.benefits.model;

import com.example.activityservice.feature.activities.model.Activities;
import com.example.activityservice.feature.categories.model.Categories;
import jakarta.persistence.*;

@Entity
@Table(name = "benefits")
public class Benefits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Activities activity;

    private Integer type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Categories category;

    private Integer point;

    public Benefits() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Activities getActivity() { return activity; }
    public void setActivity(Activities activity) { this.activity = activity; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Categories getCategory() { return category; }
    public void setCategory(Categories category) { this.category = category; }
    public Integer getPoint() { return point; }
    public void setPoint(Integer point) { this.point = point; }

    public static BenefitsBuilder builder() { return new BenefitsBuilder(); }

    public static class BenefitsBuilder {
        private Long id;
        private Activities activity;
        private Integer type;
        private Categories category;
        private Integer point;

        public BenefitsBuilder id(Long id) { this.id = id; return this; }
        public BenefitsBuilder activity(Activities activity) { this.activity = activity; return this; }
        public BenefitsBuilder type(Integer type) { this.type = type; return this; }
        public BenefitsBuilder category(Categories category) { this.category = category; return this; }
        public BenefitsBuilder point(Integer point) { this.point = point; return this; }
        public Benefits build() {
            Benefits b = new Benefits();
            b.id = this.id;
            b.activity = this.activity;
            b.type = this.type;
            b.category = this.category;
            b.point = this.point;
            return b;
        }
    }
}
