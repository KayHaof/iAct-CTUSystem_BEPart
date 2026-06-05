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
    
    @NotBlank(message = "Tieu de khong duoc de trong")
    @Size(max = 100, message = "Tieu de khong vuot qua 100 ky tu")
    private String title;
    
    @NotBlank(message = "Noi dung khong duoc de trong")
    @Size(max = 1000, message = "Noi dung khong vuot qua 1000 ky tu")
    private String message;
    
    private Integer priority;  // 1 = binh thuong, 2 = quan trong, 3 = khan cap
    
    private String targetType;  // "ALL_DEPARTMENT", "ACTIVITY", "CLASS"
    private Long targetId;      // departmentId, activityId, hoac classId
    
    private Long activityId;    // Neu co
    private String[] userIds;   // Danh sach userIds cu the (neu can)
}
