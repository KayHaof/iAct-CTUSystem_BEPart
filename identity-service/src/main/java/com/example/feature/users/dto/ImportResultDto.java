package com.example.feature.users.dto;

import lombok.Data;

@Data
public class ImportResultDto {
    private int successCount = 0;
    private int failCount = 0;
    private String errorFileBase64; // Chứa mã Base64 của file Excel chứa các dòng lỗi
}
