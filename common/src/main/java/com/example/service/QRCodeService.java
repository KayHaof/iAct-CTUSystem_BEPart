package com.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Service
public class QRCodeService {
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            // 1. Khởi tạo thuật toán vẽ QR Code của ZXing
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 2. Chuyển đổi text thành ma trận điểm ảnh (BitMatrix)
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            // 3. Viết ma trận đó ra một luồng byte (định dạng PNG)
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            // 4. Lấy mảng byte hình ảnh
            byte[] pngData = pngOutputStream.toByteArray();

            // 5. Encode thành chuỗi Base64 để nhét vào JSON gửi cho Frontend
            String base64Image = Base64.getEncoder().encodeToString(pngData);

            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            log.error("Lỗi khi tạo mã QR Code: ", e);
            return null;
        }
    }
}
