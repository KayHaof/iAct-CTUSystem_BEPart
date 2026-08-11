package com.example.feature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrgentNotificationRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 100, message = "Tiêu đề không vượt quá 100 ký tự")
    private String title;
    
    @NotBlank(message = "Nội dung không được để trống")
    @Size(max = 1000, message = "Nội dung không vượt quá 1000 ký tự")
    private String message;
    
    private Integer priority;  // 1 = bình thường, 2 = quan trọng, 3 = khẩn cấp
    
    private String targetType;  // "ALL_DEPARTMENT", "ACTIVITY", "CLASS"
    private Long targetId;      // departmentId, activityId, hoặc classId
    
    private Long activityId;    // Nếu có
    private String[] userIds;   // Danh sách userIds cụ thể (nếu cần)
}
