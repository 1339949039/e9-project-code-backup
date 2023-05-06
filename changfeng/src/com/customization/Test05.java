package com.customization;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weaver.general.Util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Li Yu Feng
 * @date 2023-04-23 10:01
 */
public class Test05 {

    //创建一个线程池 线程数量3个，最大线程5个（表示如果3个线程数量在忙着，并且队列上已经满了则会创建临时线程），存活时间8秒（如果临时线程8秒内没有新任务则销毁），秒，阻塞队列5个，
    // 创建线程池的工厂，异常如果临时线程已经在工作，并且等待队列也满了，则会抛出异常，不接受新任务
    ExecutorService threadService = new ThreadPoolExecutor(3, 5, 8L,
            TimeUnit.MINUTES, new ArrayBlockingQueue<>(5), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    private Map<String, CellStyle> cellStyleMap = new HashMap<>();


    // 创建可重入锁
    ReentrantLock lock = new ReentrantLock();

    public void readExcel(String readExcelPath, String writeExcelPath)  {
        try (
                // 创建一个输入流，读取Excel文件
                FileInputStream inputStream = new FileInputStream(readExcelPath);
                // 创建Workbook对象，表示整个Excel文件
                Workbook workbook = new XSSFWorkbook(inputStream);

        ) {
            // 获取Sheet对象，表示Excel文件中的一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            //存列名索引
            int rowIndex = 2;
            short cellNum = sheet.getRow(rowIndex).getLastCellNum();
            Map<String, Integer> rowIndexMap = new HashMap<>();
            for (int i = 0; i < cellNum; i++) {
                int columnIndex = sheet.getRow(rowIndex).getCell(i).getColumnIndex();
                String cellValue = sheet.getRow(rowIndex).getCell(i).getStringCellValue();
                if (cellValue.contains("上班") || cellValue.contains("下班")) {
                    rowIndexMap.put(cellValue + "." + sheet.getRow(rowIndex + 1).getCell(i).getStringCellValue(), sheet.getRow(rowIndex + 1).getCell(i).getColumnIndex());
                    rowIndexMap.put(cellValue + "." + sheet.getRow(rowIndex + 1).getCell(i + 1).getStringCellValue(), sheet.getRow(rowIndex + 1).getCell(i + 1).getColumnIndex());
                } else {
                    rowIndexMap.put(cellValue, columnIndex);
                }
            }
            List<Map<String, String>> rowListData = new ArrayList<>();
            int rowNum = (sheet.getLastRowNum() - 5);
            System.out.println("rowNum = " + rowNum % 100);
            System.out.println("rowIndexMap = " + rowIndexMap);
            for (int i = 0; i <= sheet.getLastRowNum() - 5; i++) {

                if (rowListData.size() >= 2000||i==sheet.getLastRowNum() - 5) {
                    while (true) {
                        if (((ThreadPoolExecutor) threadService).getActiveCount() == ((ThreadPoolExecutor) threadService).getMaximumPoolSize()) {
                            System.out.println("线程池已满，等待中...");
                            Thread.sleep(1000);
                        }else {
                            threadService.submit(new MyCallable(rowListData,writeExcelPath));
                            break;
                        }
                    }
                    rowListData = new ArrayList<>();

                }
                //第5行读取数据
                Row readrRow = sheet.getRow(i + 5);
                Map<String, String> cellData = new HashMap<>();
                cellData.put("name", getCellName("姓名", readrRow, rowIndexMap));
                cellData.put("shift", getCellName("班次", readrRow, rowIndexMap));
                cellData.put("dept", getCellName("部门", readrRow, rowIndexMap));
                cellData.put("numbering", getCellName("编号", readrRow, rowIndexMap));
                cellData.put("posts", getCellName("岗位", readrRow, rowIndexMap));
                cellData.put("date", getCellName("日期", readrRow, rowIndexMap));
                cellData.put("work1Time", getCellName("上班1.打卡时间", readrRow, rowIndexMap));
                cellData.put("work1Ar", getCellName("上班1.考勤结果", readrRow, rowIndexMap));
                cellData.put("underWork1Time", getCellName("下班1.打卡时间", readrRow, rowIndexMap));
                cellData.put("underWork1Ar", getCellName("下班1.考勤结果", readrRow, rowIndexMap));
                cellData.put("work2Time", getCellName("上班2.打卡时间", readrRow, rowIndexMap));
                cellData.put("work2Ar", getCellName("上班2.考勤结果", readrRow, rowIndexMap));
                cellData.put("underWork2Time", getCellName("下班2.打卡时间", readrRow, rowIndexMap));
                cellData.put("underWork2Ar", getCellName("下班2.考勤结果", readrRow, rowIndexMap));
                cellData.put("work3Time", getCellName("上班3.打卡时间", readrRow, rowIndexMap));
                cellData.put("work3Ar", getCellName("上班3.考勤结果", readrRow, rowIndexMap));
                cellData.put("underWork3Time", getCellName("下班3.打卡时间", readrRow, rowIndexMap));
                cellData.put("underWork3Ar", getCellName("下班3.考勤结果", readrRow, rowIndexMap));
                cellData.put("manHour", getCellName("实际工作时长(小时)", readrRow, rowIndexMap));
                rowListData.add(cellData);

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // 关闭线程池
                threadService.shutdown();
                // 等待线程池中的任务全部完成
                while (!threadService.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.out.println("线程池中的任务还未全部完成，继续等待...");
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void writeExcel(List<Map<String, String>> rowListData, String writeExcelPath) throws Exception{
        // 加锁
        lock.lock();
        try (
                FileInputStream outStream = new FileInputStream(writeExcelPath);
                Workbook workbook = new XSSFWorkbook(outStream);
                FileOutputStream outputStream = new FileOutputStream(writeExcelPath);
        ) {
            Sheet sheet = workbook.getSheetAt(0);

            initStyle(workbook);
            CellStyle style = cellStyleMap.get("standard");


            int lastRowNum = sheet.getLastRowNum();
            for (int i = 0; i < rowListData.size(); i++) {
                //从第三行写入
                Row outRow = sheet.createRow(lastRowNum+i);
                Map<String, String> dataMap = rowListData.get(i);

                Cell cell0 = outRow.createCell(0);//姓名
                cell0.setCellValue(dataMap.get("name"));
                cell0.setCellStyle(style);

                String shiftTimeStr = dataMap.get("shift");
                Cell cell1 = outRow.createCell(1);//考勤组
                cell1.setCellValue(shiftTimeStr);
                cell1.setCellStyle(style);
                Map<String, Integer> shiftTimeMap = new HashMap<>();

                if (!StrUtil.hasEmpty(shiftTimeStr)) {
                    //存储班次打卡结果索引
                    String[] shiftTimeArr = shiftTimeStr.replaceAll("[^\\d:]+", " ").trim().split("\\s+");
                    int index = 8;
                    for (int j = 0; j < shiftTimeArr.length; j++) {
                        shiftTimeMap.put(shiftTimeArr[j], index);
                        index = index + 2;
                    }
                }

                Cell cell2 = outRow.createCell(2);//部门
                cell2.setCellValue(dataMap.get("dept"));
                cell2.setCellStyle(style);

                Cell cell3 = outRow.createCell(3);//工号
                cell3.setCellValue(dataMap.get("numbering"));
                cell3.setCellStyle(style);

                Cell cell4 = outRow.createCell(4);//职位
                cell4.setCellValue(dataMap.get("posts"));
                cell4.setCellStyle(style);


                Cell cell5 = outRow.createCell(5);//日期
                String week = "";
                if (dataMap.get("date").contains("星期")) {
                    week = dataMap.get("date");
                } else {
                    week = dataMap.get("date") + " " + getWeek(dataMap.get("date"));
                }
                cell5.setCellValue(week);

                if (!StrUtil.hasEmpty(week) && week.contains("星期六") || week.contains("星期日")) {
                    cell5.setCellStyle(cellStyleMap.get("styleRed"));
                } else {
                    cell5.setCellStyle(style);
                }


                Cell cell6 = outRow.createCell(6);//班次
                cell6.setCellValue(dataMap.get("shift"));
                cell6.setCellStyle(style);


                Cell cell7 = outRow.createCell(7);//上班1打卡时间
                cell7.setCellValue(dataMap.get("work1Time"));
                cell7.setCellStyle(style);

                Cell cell8 = outRow.createCell(8);//上班1考勤结果
                cell8.setCellValue(dataMap.get("work1Ar"));
                getCellStyle(dataMap.get("work1Time"), dataMap.get("work1Ar"), cell8);


                Cell cell9 = outRow.createCell(9);//下班1打卡时间
                cell9.setCellValue(dataMap.get("underWork1Time"));
                cell9.setCellStyle(style);

                Cell cell10 = outRow.createCell(10);//下班1考勤结果
                cell10.setCellValue(dataMap.get("underWork1Ar"));
                getCellStyle(dataMap.get("underWork1Time"), dataMap.get("underWork1Ar"), cell10);


                Cell cell11 = outRow.createCell(11);//上班2打卡时间
                cell11.setCellValue(dataMap.get("work2Time"));
                cell11.setCellStyle(style);


                Cell cell12 = outRow.createCell(12);//上班2考勤结果
                cell12.setCellValue(dataMap.get("work2Ar"));
                getCellStyle(dataMap.get("work2Time"), dataMap.get("work2Ar"), cell12);


                Cell cell13 = outRow.createCell(13);//下班2打卡时间
                cell13.setCellValue(dataMap.get("underWork2Time"));
                cell13.setCellStyle(style);

                Cell cell14 = outRow.createCell(14);//下班2考勤结果
                cell14.setCellValue(dataMap.get("underWork2Ar"));
                getCellStyle(dataMap.get("underWork2Time"), dataMap.get("underWork2Ar"), cell14);

                Cell cell15 = outRow.createCell(15);//上班3打卡时间
                cell15.setCellValue(dataMap.get("work3Time"));
                cell15.setCellStyle(style);


                Cell cell16 = outRow.createCell(16);//上班3考勤结果
                cell16.setCellValue(dataMap.get("work3Ar"));
                getCellStyle(dataMap.get("work3Time"), dataMap.get("work3Ar"), cell16);

                Cell cell17 = outRow.createCell(17);//下班3打卡时间
                cell17.setCellValue(dataMap.get("underWork3Time"));
                cell17.setCellStyle(style);

                Cell cell18 = outRow.createCell(18);//下班3考勤结果
                cell18.setCellValue(dataMap.get("underWork3Ar"));
                getCellStyle(dataMap.get("underWork3Time"), dataMap.get("underWork3Ar"), cell18);

                Cell cell19 = outRow.createCell(19);//工时---实际工作时长(小时)
                cell19.setCellValue(dataMap.get("manHour"));
                cell19.setCellStyle(style);

                Cell cell20 = outRow.createCell(20);//关联的审批单
                //String approvalProcess = getApprovalProcess(rs, getCellName("编号", readrRow, rowIndexMap), week, shiftTimeMap, outRow);
                cell20.setCellValue("审批单");
                cell20.setCellStyle(style);



            }
            // 保存Excel文件
            workbook.write(outputStream);
            // 代码块
        } catch (Exception e) {
            throw e;
        } finally {
            rowListData.clear();
            // 释放锁
            lock.unlock();
        }

    }
    /**
     * 根据日期判断星期几
     *
     * @param data
     * @return
     */
    public String getWeek(String data) {
        if (StrUtil.hasEmpty(data)) {
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
    private void initStyle(Workbook workbook) {
        // 创建字体
        Font font = workbook.createFont();
        font.setFontName("新宋体");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex()); // 字体颜色：黑色

        // 创建样式
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        //设置居中
        style.setAlignment(HorizontalAlignment.CENTER);
        //设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        cellStyleMap.put("standard", style);

        // 创建字体
        Font fontRed = workbook.createFont();
        fontRed.setFontName("新宋体");
        fontRed.setFontHeightInPoints((short) 12);
        fontRed.setBold(true);
        fontRed.setColor(IndexedColors.RED.getIndex()); // 字体颜色：红色

        // 创建样式
        CellStyle styleRed = workbook.createCellStyle();
        styleRed.setFont(fontRed);
        //设置居中
        styleRed.setAlignment(HorizontalAlignment.CENTER);
        //设置边框
        styleRed.setBorderTop(BorderStyle.THIN);
        styleRed.setBorderBottom(BorderStyle.THIN);
        styleRed.setBorderLeft(BorderStyle.THIN);
        styleRed.setBorderRight(BorderStyle.THIN);
        cellStyleMap.put("styleRed", styleRed);


        //1.缺卡   未打卡&&旷工  CORAL
        Font fontCORAL = workbook.createFont();
        fontCORAL.setFontName("新宋体");
        fontCORAL.setFontHeightInPoints((short) 12);
        fontCORAL.setColor(IndexedColors.BLACK.getIndex()); // 字体颜色：黑色

        fontCORAL.setBold(true);
        // 创建样式
        CellStyle styleCORAL = workbook.createCellStyle();
        styleCORAL.setFont(fontRed);
        //设置居中
        styleCORAL.setAlignment(HorizontalAlignment.CENTER);
        //设置边框
        styleCORAL.setBorderTop(BorderStyle.THIN);
        styleCORAL.setBorderBottom(BorderStyle.THIN);
        styleCORAL.setBorderLeft(BorderStyle.THIN);
        styleCORAL.setBorderRight(BorderStyle.THIN);

        //cellStyleCORAL.cloneStyleFrom(cellStyle);
        styleCORAL.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleCORAL.setFillForegroundColor(IndexedColors.CORAL.getIndex());
        cellStyleMap.put("CORAL", styleCORAL);

        //2.迟到   无&&迟到   BRIGHT_GREEN
        Font fontstyleBRIGHT_GREEN = workbook.createFont();
        fontstyleBRIGHT_GREEN.setFontName("新宋体");
        fontstyleBRIGHT_GREEN.setFontHeightInPoints((short) 12);
        fontstyleBRIGHT_GREEN.setColor(IndexedColors.BLACK.getIndex()); // 字体颜色：黑色
        fontstyleBRIGHT_GREEN.setBold(true);
        // 创建样式
        CellStyle styleBRIGHT_GREEN = workbook.createCellStyle();
        styleBRIGHT_GREEN.setFont(fontstyleBRIGHT_GREEN);
        //设置居中
        styleBRIGHT_GREEN.setAlignment(HorizontalAlignment.CENTER);
        //设置边框
        styleBRIGHT_GREEN.setBorderTop(BorderStyle.THIN);
        styleBRIGHT_GREEN.setBorderBottom(BorderStyle.THIN);
        styleBRIGHT_GREEN.setBorderLeft(BorderStyle.THIN);
        styleBRIGHT_GREEN.setBorderRight(BorderStyle.THIN);
        styleBRIGHT_GREEN.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleBRIGHT_GREEN.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());

        cellStyleMap.put("BRIGHT_GREEN", styleBRIGHT_GREEN);


        //3.早退   无&&早退   LIGHT_YELLOW
        Font fontstyleLIGHT_YELLOW = workbook.createFont();
        fontstyleLIGHT_YELLOW.setFontName("新宋体");
        fontstyleLIGHT_YELLOW.setFontHeightInPoints((short) 12);
        fontstyleLIGHT_YELLOW.setColor(IndexedColors.BLACK.getIndex()); // 字体颜色：黑色

        fontstyleLIGHT_YELLOW.setBold(true);
        // 创建样式
        CellStyle styleLIGHT_YELLOW = workbook.createCellStyle();
        styleLIGHT_YELLOW.setFont(fontstyleLIGHT_YELLOW);
        //设置居中
        styleLIGHT_YELLOW.setAlignment(HorizontalAlignment.CENTER);
        //设置边框
        styleLIGHT_YELLOW.setBorderTop(BorderStyle.THIN);
        styleLIGHT_YELLOW.setBorderBottom(BorderStyle.THIN);
        styleLIGHT_YELLOW.setBorderLeft(BorderStyle.THIN);
        styleLIGHT_YELLOW.setBorderRight(BorderStyle.THIN);
        styleLIGHT_YELLOW.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleLIGHT_YELLOW.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        cellStyleMap.put("LIGHT_YELLOW", styleLIGHT_YELLOW);


        //事假，外出
        Font fontTakeTimeOffWorkToGoOut = workbook.createFont();
        fontTakeTimeOffWorkToGoOut.setFontName("新宋体");
        fontTakeTimeOffWorkToGoOut.setFontHeightInPoints((short) 12);
        fontTakeTimeOffWorkToGoOut.setColor(IndexedColors.BLACK.getIndex()); // 字体颜色：黑色

        fontTakeTimeOffWorkToGoOut.setBold(true);
        // 创建样式
        CellStyle styleTakeTimeOffWorkToGoOut = workbook.createCellStyle();
        styleTakeTimeOffWorkToGoOut.setFont(fontTakeTimeOffWorkToGoOut);
        //设置居中
        styleTakeTimeOffWorkToGoOut.setAlignment(HorizontalAlignment.CENTER);
        //设置边框
        styleTakeTimeOffWorkToGoOut.setBorderTop(BorderStyle.THIN);
        styleTakeTimeOffWorkToGoOut.setBorderBottom(BorderStyle.THIN);
        styleTakeTimeOffWorkToGoOut.setBorderLeft(BorderStyle.THIN);
        styleTakeTimeOffWorkToGoOut.setBorderRight(BorderStyle.THIN);
        styleTakeTimeOffWorkToGoOut.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleTakeTimeOffWorkToGoOut.setFillForegroundColor(IndexedColors.TAN.getIndex());
        cellStyleMap.put("TakeTimeOffWorkToGoOut", styleTakeTimeOffWorkToGoOut);


    }
    /**
     * 根据考勤结果设置不同的样式
     *
     * @param clockTime    打卡时间
     * @param clockOutcome 考勤结果
     * @param cell         设置单元格
     */
    public void getCellStyle(String clockTime, String clockOutcome, Cell cell) {
        //1.缺卡   未打卡&&旷工  CORAL
        //2.迟到   无&&迟到   BRIGHT_GREEN
        //3.早退   无&&早退   LIGHT_YELLOW
        //4.补卡审批通过
        // 重点：从现有样式克隆style，只修改Font，其它style不变

        //1.缺卡   未打卡&&旷工  CORAL

        if (!StrUtil.hasEmpty(clockTime) && clockTime.equals("未打卡") && clockOutcome.contains("旷工")) {
            cell.setCellStyle(cellStyleMap.get("CORAL"));
        } else if (clockOutcome.contains("迟到")) {
            cell.setCellStyle(cellStyleMap.get("BRIGHT_GREEN"));

        } else if (clockOutcome.contains("早退")) {
            cell.setCellStyle(cellStyleMap.get("LIGHT_YELLOW"));
        } else if (clockOutcome.contains("漏签")) {
            cell.setCellStyle(cellStyleMap.get("CORAL"));
        } else {
            cell.setCellStyle(cellStyleMap.get("standard"));

        }
    }

    class MyCallable implements Callable<Boolean> {
        List<Map<String, String>> rowListData = null;
        String writeExcelPath;

        public MyCallable(List<Map<String, String>> rowListData, String writeExcelPath) {
            this.rowListData = rowListData;
            this.writeExcelPath = writeExcelPath;
        }

        @Override
        public Boolean call() throws Exception {
            writeExcel(rowListData, writeExcelPath);
            return true;
        }
    }

    /**
     * 根据列名名称获取列名value值
     *
     * @param cellValue   列名名称
     * @param readrRow    列内容
     * @param rowIndexMap 标题索引map
     * @return
     */
    public String getCellName(String cellValue, Row readrRow, Map<String, Integer> rowIndexMap) {
        if (StrUtil.hasEmpty(cellValue) || rowIndexMap.get(cellValue) == null) {
            return "";
        }
        return readrRow.getCell(rowIndexMap.get(cellValue)).getStringCellValue();
    }



    public static void main(String[] args) throws InterruptedException {
       /* long start = System.currentTimeMillis();
        new Test05().readExcel("F:\\code\\changfeng\\xlsx\\f5950839-4906-40ca-a1e9-ecc85b5dfab8(20230419120736).xlsx","F:\\code\\changfeng\\xlsx\\meir.xlsx");
        long end = System.currentTimeMillis();
        System.out.println("耗费 = " + (end - start) / 1000.00);*/
        // 2023-04-09 08:00到2023-04-10 17:30
        String date="2023-04-21";
        String serialid = Util.null2String("工厂办公室(08:00-11:45 13:15-17:30)");
        Map<String, String> shiftTimeMap = new HashMap<>();
        if (!StrUtil.hasEmpty(serialid)) {
            //上班 下班字段
            String[] shift=new String[]{"signinstatus1","signoutstatus1","signinstatus2","signoutstatus2","signinstatus3","signoutstatus3"};
            //存储班次打卡结果索引
            String[] shiftTimeArr = serialid.replaceAll("[^\\d:]+", " ").trim().split("\\s+");
            for (int j = 0; j < shiftTimeArr.length; j++) {
                shiftTimeMap.put(shiftTimeArr[j], shift[j]);
            }
        }
        System.out.println("shiftTimeMap = " + shiftTimeMap);
        Date dateToDate1 = cn.hutool.core.date.DateUtil.parse("2023-04-21 08:00");
        Date dateToDate2 = cn.hutool.core.date.DateUtil.parse("2023-04-23 17:30");
        long betweenDay = DateUtil.between(dateToDate1, dateToDate2, DateUnit.HOUR);

        int divisor = 24;
        long quotient = betweenDay / divisor;
        long remainder = betweenDay % divisor;

        if (remainder > 0) {
            quotient++;
        }
        System.out.println("Result: " + quotient);
        LocalDateTime startDateTime = LocalDateTime.parse("2023-04-21 08:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        LocalDateTime endDateTime = LocalDateTime.parse("2023-04-23 17:30", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String bakDate=date;
        for (int i = 0; i <= quotient; i++) {
            //获取当前行日期
            if (StrUtil.hasEmpty(date)) {
                continue;
            }
            date = date.substring(0, 10).trim();
            Date dateToDate = DateUtil.parse(date);
            //时间偏移单位天
            date = DateUtil.offset(dateToDate, DateField.DAY_OF_MONTH, i).toString().substring(0, 10).trim();
            for (String key : shiftTimeMap.keySet()) {
                LocalDateTime currentDateTime = LocalDateTime.parse(date + " " + key, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                //存样式，等单元格创建完成后再新增样式
                if (isTimeBetweenStartAndEndTime(startDateTime, endDateTime, currentDateTime)) {
                    System.out.println(date+shiftTimeMap.get(key)+"TakeTimeOffWorkToGoOut");
                }
            }
            //循环结束需要重置回来原来的日期
            date=bakDate;
        }


    }
    public static boolean isTimeBetweenStartAndEndTime(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime timeToCheck) {
        return !timeToCheck.isBefore(startTime) && !timeToCheck.isAfter(endTime);
    }
}
