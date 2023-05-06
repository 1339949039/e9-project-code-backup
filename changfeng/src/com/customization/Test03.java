package com.customization;

import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * @author Li Yu Feng
 * @date 2023-04-17 15:41
 */
public class Test03 {
    //根据日期获取当前星期几 yyyy-mm-dd
    public static String getWeek(String data){
        if (StrUtil.hasEmpty(data)){
            return "";
        }
        // 获取当前日期
        LocalDate today = LocalDate.parse(data);
        // 获取星期几
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        // 格式化日期并输出中文星期几
        String dayOfWeekInChinese = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA);
        return dayOfWeekInChinese;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String outFile = "F:\\code\\changfeng\\xlsx\\meir.xlsx";

        String filename = "F:\\code\\changfeng\\xlsx\\a3c60c82-e2c0-4b23-b12e-072c43eff6b5(20230414135849).xlsx";
        try (FileInputStream fis = new FileInputStream(filename);
             Workbook workbook = new XSSFWorkbook(fis);
             FileInputStream outStream = new FileInputStream(outFile);
             Workbook outWorkbook = new XSSFWorkbook(outStream);
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            Sheet outSheet = outWorkbook.getSheetAt(0);

            Cell cell = outSheet.getRow(0).getCell(0);
            cell.setCellValue("修改第一列");
            CellStyle cellStyle = outSheet.getRow(3).getCell(0).getCellStyle();

            //1.缺卡   未打卡&&旷工  CORAL
            CellStyle cellStyleCORAL=outWorkbook.createCellStyle();
            cellStyleCORAL.cloneStyleFrom(cellStyle);
            cellStyleCORAL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellStyleCORAL.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            //2.迟到   无&&迟到   BRIGHT_GREEN
            CellStyle cellStyleBRIGHT_GREEN=outWorkbook.createCellStyle();
            cellStyleBRIGHT_GREEN.cloneStyleFrom(cellStyle);
            cellStyleBRIGHT_GREEN.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellStyleBRIGHT_GREEN.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
            //3.早退   无&&早退   LIGHT_YELLOW
            CellStyle cellStyleLIGHT_YELLOW=outWorkbook.createCellStyle();
            cellStyleLIGHT_YELLOW.cloneStyleFrom(cellStyle);
            cellStyleLIGHT_YELLOW.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellStyleLIGHT_YELLOW.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            //存列名索引
            int rowIndex=2;
            short cellNum = sheet.getRow(rowIndex).getLastCellNum();
            Map<String,Integer> rowIndexMap=new HashMap<>();
            for (int i = 0; i < cellNum; i++) {
                int columnIndex = sheet.getRow(rowIndex).getCell(i).getColumnIndex();
                String cellValue = sheet.getRow(rowIndex).getCell(i).getStringCellValue();
                if (cellValue.contains("上班")||cellValue.contains("下班")){
                    rowIndexMap.put(cellValue+"."+sheet.getRow(rowIndex+1).getCell(i).getStringCellValue(),sheet.getRow(rowIndex+1).getCell(i).getColumnIndex());
                    rowIndexMap.put(cellValue+"."+sheet.getRow(rowIndex+1).getCell(i+1).getStringCellValue(),sheet.getRow(rowIndex+1).getCell(i+1).getColumnIndex());
                }else {
                    rowIndexMap.put(cellValue,columnIndex);
                }
            }


            for (int i = 0; i < sheet.getLastRowNum()-5; i++) {
                //第5行读取数据
                Row readrRow = sheet.getRow(i+5);
                //第三行写入
                Row outRow = outSheet.createRow(i + 3);

                Cell cell0 = outRow.createCell(0);//姓名
                cell0.setCellValue(getCellName("姓名",readrRow,rowIndexMap));
                cell0.setCellStyle(cellStyle);

                Cell cell1 = outRow.createCell(1);//考勤组
                cell1.setCellValue(getCellName("班次",readrRow,rowIndexMap));
                cell1.setCellStyle(cellStyle);

                Cell cell2 = outRow.createCell(2);//部门
                cell2.setCellValue(getCellName("部门",readrRow,rowIndexMap));
                cell2.setCellStyle(cellStyle);

                Cell cell3 = outRow.createCell(3);//工号
                cell3.setCellValue("工号暂时给空");
                cell3.setCellStyle(cellStyle);

                Cell cell4 = outRow.createCell(4);//职位
                cell4.setCellValue(getCellName("岗位",readrRow,rowIndexMap));
                cell4.setCellStyle(cellStyle);


                Cell cell5 = outRow.createCell(5);//日期
                String week = "";
                if (getCellName("日期",readrRow,rowIndexMap).contains("星期")){
                    week=getCellName("日期",readrRow,rowIndexMap);
                }else {
                    week=getWeek(getCellName("日期",readrRow,rowIndexMap));
                }
                cell5.setCellValue(week);
                if (!StrUtil.hasEmpty(week)&&week.contains("星期六")||week.contains("星期日")){
                    CellStyle cellStyle5 = outWorkbook.createCellStyle();
                    // 重点：从现有样式克隆style，只修改Font，其它style不变
                    cellStyle5.cloneStyleFrom(cellStyle);
                    // 获取原有字体
                    Font oldFont = outWorkbook.getFontAt(cellStyle.getFontIndexAsInt());
                    // 创建新字体
                    Font redFont = outWorkbook.createFont();
                    // 重点：保留原字体样式
                    redFont.setFontName(oldFont.getFontName()); // 保留原字体
                    redFont.setFontHeightInPoints(oldFont.getFontHeightInPoints()); // 保留原字体高度
                    redFont.setBold(false); // 加粗
                    redFont.setColor(IndexedColors.RED.getIndex());  // 字体颜色：红色
                    // 设置红色字体
                    cellStyle5.setFont(redFont);
                    cell5.setCellStyle(cellStyle5);

                }else {
                    cell5.setCellStyle(cellStyle);
                }


                Cell cell6 = outRow.createCell(6);//班次
                cell6.setCellValue(getCellName("班次",readrRow,rowIndexMap));
                cell6.setCellStyle(cellStyle);


                Cell cell7 = outRow.createCell(7);//上班1打卡时间
                cell7.setCellValue(getCellName("上班1.打卡时间",readrRow,rowIndexMap));
                cell7.setCellStyle(cellStyle);

                Cell cell8 = outRow.createCell(8);//上班1考勤结果
                cell8.setCellValue(getCellName("上班1.考勤结果",readrRow,rowIndexMap));
                getCellStyle(getCellName("上班1.打卡时间",readrRow,rowIndexMap),getCellName("上班1.考勤结果",readrRow,rowIndexMap),cell8,cellStyle,cellStyleCORAL,cellStyleBRIGHT_GREEN,cellStyleLIGHT_YELLOW);


                Cell cell9 = outRow.createCell(9);//下班1打卡时间
                cell9.setCellValue(getCellName("下班1.打卡时间",readrRow,rowIndexMap));
                cell9.setCellStyle(cellStyle);

                Cell cell10 = outRow.createCell(10);//下班1考勤结果
                cell10.setCellValue(getCellName("下班1.考勤结果",readrRow,rowIndexMap));
                getCellStyle(getCellName("下班1.打卡时间",readrRow,rowIndexMap),getCellName("下班1.考勤结果",readrRow,rowIndexMap),cell10,cellStyle,cellStyleCORAL,cellStyleBRIGHT_GREEN,cellStyleLIGHT_YELLOW);



                Cell cell11 = outRow.createCell(11);//上班2打卡时间
                cell11.setCellValue(getCellName("上班2.打卡时间",readrRow,rowIndexMap));
                cell11.setCellStyle(cellStyle);


                Cell cell12 = outRow.createCell(12);//上班2考勤结果
                cell12.setCellValue(getCellName("上班2.考勤结果",readrRow,rowIndexMap));
                getCellStyle(getCellName("上班2.打卡时间",readrRow,rowIndexMap),getCellName("上班2.考勤结果",readrRow,rowIndexMap),cell12,cellStyle,cellStyleCORAL,cellStyleBRIGHT_GREEN,cellStyleLIGHT_YELLOW);


                Cell cell13 = outRow.createCell(13);//下班2打卡时间
                cell13.setCellValue(getCellName("下班2.打卡时间",readrRow,rowIndexMap));
                cell13.setCellStyle(cellStyle);

                Cell cell14 = outRow.createCell(14);//下班2考勤结果
                cell14.setCellValue(getCellName("下班2.考勤结果",readrRow,rowIndexMap));
                getCellStyle(getCellName("下班2.打卡时间",readrRow,rowIndexMap),getCellName("下班2.考勤结果",readrRow,rowIndexMap),cell14,cellStyle,cellStyleCORAL,cellStyleBRIGHT_GREEN,cellStyleLIGHT_YELLOW);

                Cell cell15 = outRow.createCell(15);//上班3打卡时间
                cell15.setCellValue(getCellName("上班3.打卡时间",readrRow,rowIndexMap));
                cell15.setCellStyle(cellStyle);


                Cell cell16 = outRow.createCell(16);//上班3考勤结果
                cell16.setCellValue(getCellName("上班3.考勤结果",readrRow,rowIndexMap));
                getCellStyle(getCellName("上班3.打卡时间",readrRow,rowIndexMap),getCellName("上班3.考勤结果",readrRow,rowIndexMap),cell16,cellStyle,cellStyleCORAL,cellStyleBRIGHT_GREEN,cellStyleLIGHT_YELLOW);

                Cell cell17 = outRow.createCell(17);//下班3打卡时间
                cell17.setCellValue(getCellName("下班3.打卡时间",readrRow,rowIndexMap));
                cell17.setCellStyle(cellStyle);

                Cell cell18 = outRow.createCell(18);//下班3考勤结果
                cell18.setCellValue(getCellName("下班3.考勤结果",readrRow,rowIndexMap));
                getCellStyle(getCellName("下班3.打卡时间",readrRow,rowIndexMap),getCellName("下班3.考勤结果",readrRow,rowIndexMap),cell18,cellStyle,cellStyleCORAL,cellStyleBRIGHT_GREEN,cellStyleLIGHT_YELLOW);

                Cell cell19 = outRow.createCell(19);//工时---实际工作时长(小时)
                cell19.setCellValue(getCellName("实际工作时长(小时)",readrRow,rowIndexMap));
                cell19.setCellStyle(cellStyle);

                Cell cell20 = outRow.createCell(20);//关联的审批单
                cell20.setCellValue("关联的审批单暂时为空");
                cell20.setCellStyle(cellStyle);


            }
            FileOutputStream outputStream = new FileOutputStream(outFile);
            outWorkbook.write(outputStream);
            System.out.println(" 更新完成 " );
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            long end = System.currentTimeMillis();
            System.out.println("耗时: " + (end-start)/1000.00);

        }
    }

    public static void getCellStyle(String clockTime, String clockOutcome, Cell cell, CellStyle cellStyle,CellStyle cellStyleCORAL,CellStyle cellStyleBRIGHT_GREEN,CellStyle cellStyleLIGHT_YELLOW){
        //1.缺卡   未打卡&&旷工  CORAL
        //2.迟到   无&&迟到   BRIGHT_GREEN
        //3.早退   无&&早退   LIGHT_YELLOW
        //4.补卡审批通过
        // 重点：从现有样式克隆style，只修改Font，其它style不变
        if (!StrUtil.hasEmpty(clockTime)&&clockTime.equals("未打卡")&&clockOutcome.contains("旷工")){
            cell.setCellStyle(cellStyleCORAL);
        }else if (clockOutcome.contains("迟到")){
            cell.setCellStyle(cellStyleBRIGHT_GREEN);
        } else if (clockOutcome.contains("早退")){
            cell.setCellStyle(cellStyleLIGHT_YELLOW);
        }else if(clockOutcome.contains("漏签")){
            cell.setCellStyle(cellStyleCORAL);
        }else  {
            cell.setCellStyle(cellStyle);

        }
    }
    public static String getCellName(String cellValue, Row readrRow, Map<String, Integer> rowIndexMap){
        if (StrUtil.hasEmpty(cellValue)||rowIndexMap.get(cellValue)==null){
            return "";
        }
        return readrRow.getCell(rowIndexMap.get(cellValue)).getStringCellValue();
    }
}
