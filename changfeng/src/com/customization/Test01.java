package com.customization;

import cn.hutool.json.JSONUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import weaver.general.TimeUtil;
import weaver.general.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Li Yu Feng
 * @date 2023-04-14 14:16
 */
public class Test01 {
    public static void main(String[] args) throws IOException {
        String fileName = "F:\\code\\changfeng\\xlsx\\a3c60c82-e2c0-4b23-b12e-072c43eff6b5(20230414135849).xlsx";
        File file = new File(fileName);
        Test01 test01 = new Test01();
        Map<String, Object> parms = new HashMap<>();
        test01.updataKqReport(parms, file);

    }

    public void updataKqReport(Map<String, Object> parms, File file) throws IOException {
        String typeselect = Util.null2String(parms.get("typeselect"));

        if (typeselect.length() == 0) {
            typeselect = "3";
        }
        String startDateTime = "";
        String endDateTime = "";
        //获取统计开始时间和结束时间
        if (!typeselect.equals("") && !typeselect.equals("0") && !typeselect.equals("6")) {
            if (typeselect.equals("1")) {
                startDateTime = TimeUtil.getCurrentDateString();
                endDateTime = TimeUtil.getCurrentDateString();
            } else {
                startDateTime = TimeUtil.getDateByOption(typeselect, "0");
                endDateTime = TimeUtil.getDateByOption(typeselect, "1");
            }
        }
        FileInputStream inputStream = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();

        //取消合并
        sheet.removeMergedRegion(0);
        // 创建合并单元格区域
        CellRangeAddress mergedRegion = new CellRangeAddress(0, 1, 0, 52);

        // 将单元格合并为一个单元格
        sheet.addMergedRegion(mergedRegion);

        // 创建单元格样式
        CellStyle cellStyle = workbook.createCellStyle();

        // 设置水平居中和垂直居中
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 创建字体
        Font font = workbook.createFont();
        font.setFontName("新宋体");
        font.setFontHeightInPoints((short) 24);

        // 设置字体和文字大小
        cellStyle.setFont(font);


        // 设置背景颜色
        cellStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);


        // 获取要设置样式的单元格
//        Cell cell = sheet.getRow(0).createCell(0);
//
//        // 设置单元格样式
//        cell.setCellStyle(cellStyle);
//        cell.setCellValue("每日统计报表  统计日期:"+startDateTime+" 至 "+endDateTime);







        //Row row = sheet.getRow(0);
       //row.getCell(0).setCellValue();

//        //设置标题颜色
//        for (int i = 2; i <5; i++) {
//            Cell cellRow234 = sheet.getRow(i).getCell(0);
//            CellStyle cellStyle234 = cellRow234.getCellStyle();
//            cellStyle234.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
//            cellStyle234.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//            cellRow234.setCellStyle(cellStyle234);
//        }



        // 保存更新后的工作簿
        FileOutputStream outputStream = new FileOutputStream(file);
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
        inputStream.close();

        System.out.println("Excel 文件更新成功！");


    }

    /**
     * rgb转int
     */
    private static int getIntFromColor(int Red, int Green, int Blue){
        Red = (Red << 16) & 0x00FF0000;
        Green = (Green << 8) & 0x0000FF00;
        Blue = Blue & 0x000000FF;
        return 0xFF000000 | Red | Green | Blue;
    }

    /**
     * int转byte[]
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }
}
