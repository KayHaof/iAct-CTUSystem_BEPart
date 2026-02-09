package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {

    private Long userId;        // ID người nhận (User bị khóa)
    private String title;       // Tiêu đề (VD: "Thông báo hệ thống")
    private String message;     // Nội dung (VD: "Tài khoản bị vô hiệu hóa...")
    private Integer type;       // Loại 99 (Force Logout)

}