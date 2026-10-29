package com.greathiit.bwc.common;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出（xlsx）。
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static void write(HttpServletResponse response, String fileName, List<String> headers,
                             List<String> keys, List<? extends Map<String, ?>> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("data");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            int rowIndex = 1;
            for (Map<String, ?> data : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < keys.size(); i++) {
                    Object value = data.get(keys.get(i));
                    row.createCell(i).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded + ".xlsx");
            workbook.write(response.getOutputStream());
        }
    }
}
