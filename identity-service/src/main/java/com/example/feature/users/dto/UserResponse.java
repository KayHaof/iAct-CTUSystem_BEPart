package com.example.feature.users.dto;

import lombok.*;
import java.time.LocalDate; // Dùng LocalDate cho ngày sinh
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;            // Cần ID để update sau này
    private String username;
    private String email;
    private String fullName;
    private String studentCode;

    private Integer roleType;   // 1=student, 2=dept...
    private Integer status;     // 1=active, 0=inactive...

    private String avatarUrl;

    // Thông tin cá nhân mới thêm
    private LocalDate birthday; // Backend trả về dạng [2003, 5, 20] hoặc "2003-05-20"
    private Integer gender;     // 1=Nam, 0=Nữ
    private String phone;
    private String address;

    // Thông tin lớp/khoa (Lấy cả ID và Tên/Mã)
    private Long classId;
    private String classCode;

    private Long departmentId;
    private String departmentName;

    private LocalDateTime createdAt; // Có thể cần hiển thị ngày tham gia
}