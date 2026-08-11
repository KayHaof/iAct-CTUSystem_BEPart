package com.example.activityservice.feature.activities.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ActivityContentGenerationService {

    public String generateDescription(String prompt) {
        if (prompt == null || prompt.trim().isBlank()) {
            return "Vui lòng nhập mô tả để iAct tạo nội dung hoạt động.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Nội dung hoạt động được gợi ý\n\n");
        sb.append("## Tiêu đề gợi ý\n");
        sb.append("- ").append(prompt).append("\n\n");

        sb.append("## Mô tả tóm tắt\n");
        String[] words = prompt.split("\\s+");
        if (words.length > 3) {
            sb.append("Hoạt động \"");
            for (int i = 0; i < Math.min(5, words.length); i++) {
                sb.append(words[i]).append(" ");
            }
            sb.append("...\" nhằm mang lại ");
            if (words.length > 5) {
                sb.append("kiến thức và kỹ năng thực tế").append(" cho sinh viên.\n");
            } else {
                sb.append("trải nghiệm học tập").append(" cho sinh viên.\n");
            }
        }

        sb.append("\n## Mục tiêu\n");
        sb.append("- Giúp sinh viên hiểu rõ về chủ đề: ").append(prompt).append("\n");
        sb.append("- Phát triển kỹ năng thực hành\n");
        sb.append("- Tạo cơ hội giao lưu và học hỏi\n\n");

        sb.append("## Nội dung chính\n");
        sb.append("1. Giới thiệu tổng quan về chủ đề\n");
        sb.append("2. Hướng dẫn và thực hành\n");
        sb.append("3. Thảo luận và trả lời lời\n");
        sb.append("4. Tổng kết và rút kinh nghiệm\n\n");

        sb.append("## Kết luận\n");
        sb.append("Hoạt động mang tính thực tế cao, phù hợp với sinh viên\n");

        log.info("Generated AI description for prompt: {}", prompt);
        return sb.toString();
    }
}
