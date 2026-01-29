package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenefitDto {
    private Long id;
    private Integer type;     // 1=point, 2=certificate
    private Integer point;    // Điểm số
    private Long categoryId;
}
