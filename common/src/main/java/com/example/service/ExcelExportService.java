package com.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

@Component
public class ExcelExportService {

    public <T> void export(String sheetName, String[] headers, List<T> data,
                           Function<T, Object[]> rowMapper, OutputStream os) throws Exception {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Kẻ viền đen xung quanh ô Header
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Căn giữa chữ
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 3. Ghi dòng Header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 4. Ghi dữ liệu
            int rowIndex = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowIndex++);
                Object[] cellValues = rowMapper.apply(item);

                for (int i = 0; i < cellValues.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellStyle(dataStyle);

                    Object value = cellValues[i];

                    // Tự động ép kiểu và ghi giá trị
                    switch (value) {
                        case null -> cell.setCellValue("");
                        case String s -> cell.setCellValue(s);
                        case Number number -> cell.setCellValue(number.doubleValue());
                        case Boolean b -> cell.setCellValue(b);
                        case LocalDateTime localDateTime -> cell.setCellValue(localDateTime.format(dateFormatter));
                        default -> cell.setCellValue(value.toString());
                    }
                }
            }

            // 5. Tự động nới rộng các cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 6. Đẩy ra luồng tải về
            workbook.write(os);
        }
    }
}