package com.example.common.entity;

import com.example.feature.semesters.model.Semesters;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activities") // Bảng này do Kafka tự động insert/update
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Activities {
    @Id
    private Long id;

    private String title;

    // Giữ lại trạng thái để nếu Hoạt động bị Hủy, thì bên này không cho thêm Quyền lợi
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Semesters semester;

    @Column(name = "organizer_id")
    private Long organizerId;

    @Column(name = "created_by_username")
    private String createdByUsername; // Thêm trường này thay vì User Entity để dễ check Quyền cá nhân
}