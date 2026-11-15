package com.campus.activity.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出工具（POI），供报名名单与签到记录复用。
 */
public class ExcelUtil {

    public static void export(HttpServletResponse response, String fileName,
                              List<String> headers, List<List<String>> rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("sheet1");
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                head.createCell(i).setCellValue(headers.get(i));
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> cells = rows.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    row.createCell(c).setCellValue(cells.get(c));
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename="
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".xlsx");
            wb.write(response.getOutputStream());
        }
    }
}
