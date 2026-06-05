package com.example.activityservice.feature.student_awards.model;

import com.example.activityservice.feature.awards.model.Awards;
import com.example.activityservice.feature.semesters.model.Semesters;
import com.example.activityservice.feature.users.model.Users;
import jakarta.persistence.*;

@Entity
@Table(name = "student_awards")
public class Student_awards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Users student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id")
    private Awards award;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semesters semester;

    private Integer status;

    public Student_awards() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Users getStudent() { return student; }
    public void setStudent(Users student) { this.student = student; }
    public Awards getAward() { return award; }
    public void setAward(Awards award) { this.award = award; }
    public Semesters getSemester() { return semester; }
    public void setSemester(Semesters semester) { this.semester = semester; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
