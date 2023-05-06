package com.customization;

import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Li Yu Feng
 * @date 2023-04-17 14:46
 */
public class Test02 {
    public static void main(String[] args) throws Exception {
        File file = new File("F:\\code\\changfeng\\xlsx\\meir.xlsx");
        FileInputStream inputStream = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.createRow(5);
        //19
        Cell cell = row.createCell(0);
        cell.setCellValue("李玉锋");
        cell.setCellStyle(sheet.getRow(4).getCell(0).getCellStyle());


        // 保存更新后的工作簿
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
        inputStream.close();

        System.out.println("Excel 文件更新成功！");
    }

    public void update() {
        String filename = "F:\\code\\changfeng\\xlsx\\a3c60c82-e2c0-4b23-b12e-072c43eff6b5(20230414135849).xlsx";
        try (FileInputStream fis = new FileInputStream(filename);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> cellValue = new ArrayList<>();
                for (Cell cell : row) {
                    cellValue.add(cell.getStringCellValue());
                }
                System.out.println("cellValue:" + cellValue);
                cellValue.removeAll(cellValue);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
