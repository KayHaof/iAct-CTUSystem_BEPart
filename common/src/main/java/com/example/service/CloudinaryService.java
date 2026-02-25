package com.example.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // 2. Hàm cắt chuỗi (Vẫn giữ nguyên như cũ của ní)
    public static String extractPublicId(String url) {
        if (url == null || !url.contains("/upload/")) {
            return null;
        }
        try {
            String afterUpload = url.split("/upload/")[1];
            String idWithExtension = afterUpload.substring(afterUpload.indexOf("/") + 1);
            return idWithExtension.substring(0, idWithExtension.lastIndexOf("."));
        } catch (Exception e) {
            System.err.println("Lỗi phân tích URL Cloudinary: " + e.getMessage());
            return null;
        }
    }

    // 3. THÊM HÀM NÀY: Hàm thực hiện việc xóa ảnh
    public void deleteImageByUrl(String oldAvatarUrl) {
        String publicId = extractPublicId(oldAvatarUrl);

        if (publicId != null) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("Đã dọn dẹp ảnh cũ trên Cloud: " + publicId);
            } catch (Exception e) {
                System.err.println("Không thể xóa ảnh cũ: " + e.getMessage());
            }
        }
    }
}