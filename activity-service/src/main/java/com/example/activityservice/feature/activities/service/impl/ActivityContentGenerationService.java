package com.example.activityservice.feature.activities.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ActivityContentGenerationService {

    public String generateDescription(String prompt) {
        if (prompt == null || prompt.trim().isBlank()) {
            return "Vui long nhap mo ta de iAct tao noi dung hoat dong.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Noi dung hoat dong duoc goi y\n\n");
        sb.append("## Tieu de goi y\n");
        sb.append("- ").append(prompt).append("\n\n");

        sb.append("## Mo ta tom tat\n");
        String[] words = prompt.split("\\s+");
        if (words.length > 3) {
            sb.append("Hoat dong \"");
            for (int i = 0; i < Math.min(5, words.length); i++) {
                sb.append(words[i]).append(" ");
            }
            sb.append("...\" nham mang lai ");
            if (words.length > 5) {
                sb.append("kien thuc va ky nang thuc te").append(" cho sinh vien.\n");
            } else {
                sb.append("trai nghiem hoc tap").append(" cho sinh vien.\n");
            }
        }

        sb.append("\n## Muc tieu\n");
        sb.append("- Giup sinh vien hieu ro ve chu de: ").append(prompt).append("\n");
        sb.append("- Phat trien ky nang thuc hanh\n");
        sb.append("- Tao co hoi giao luu va hoc hoi\n\n");

        sb.append("## Noi dung chinh\n");
        sb.append("1. Gioi thieu tong quan ve chu de\n");
        sb.append("2. Huong dan va thuc hanh\n");
        sb.append("3. Tha luon va tra loi loi\n");
        sb.append("4. Tong ket va rut kinh nghiem\n\n");

        sb.append("## Ket luan\n");
        sb.append("Hoat dong mang tinh thuc te cao, phu hop voi sinh vien\n");

        log.info("Generated AI description for prompt: {}", prompt);
        return sb.toString();
    }
}
