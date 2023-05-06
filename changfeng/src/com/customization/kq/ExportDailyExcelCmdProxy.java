package com.customization.kq;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.engine.core.cfg.annotation.CommandDynamicProxy;
import com.engine.core.interceptor.AbstractCommandProxy;
import com.engine.core.interceptor.Command;
import com.engine.kq.cmd.report.ExportDailyExcelCmd;
import com.icbc.api.internal.apache.http.impl.cookie.S;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weaver.conn.RecordSet;
import weaver.general.*;
import weaver.hrm.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;


/**
 * @author Li Yu Feng
 * @date 2022-08-21 11:04
 */
@CommandDynamicProxy(target = ExportDailyExcelCmd.class, desc = "每日统计报表二次开发")
public class ExportDailyExcelCmdProxy extends AbstractCommandProxy<Map<String, Object>> {
    private BaseBean log = new BaseBean();
    private Map<String, User> userInfo = new HashMap<>();

    private Map<String, CellStyle> cellStyleMap = new HashMap<>();

    private List<Map<String, String>> workflow = new ArrayList<>();

    //存当前人的流程id,避免重复查询数据库
    private Map<Integer, List<String>> currentRequestIdsMap = new HashMap<>();
    //缓存外出类型
    private Map<String, String> outOfOfficeType = new HashMap<>();
    //待修改的样式
    Map<Integer, List<Integer>> toBeModifiedStyleMap = new HashMap<>();



    //存储请假类型
    Map<String, String> typeOfLeave = new HashMap<>();

    @Override
    public Map<String, Object> execute(Command<Map<String, Object>> command) {
        log.writeLog("ExportDailyExcelCmd代理: start");
        long start = System.currentTimeMillis();
        ExportDailyExcelCmd cmd = (ExportDailyExcelCmd) command;//获取代理对象
        Map<String, Object> params = cmd.getParams();//代理前的参数
        log.writeLog("ExportDailyExcelCmd代理参数前:" + JSONObject.toJSONString(params));
        String data = Util.null2String(params.get("data"));
        JSONObject dataObject = JSON.parseObject(data);
        String showColumns = Util.null2String(dataObject.get("showColumns"));
        //没勾选岗位
        if (!showColumns.contains("jobtitle")) {
            showColumns += ",jobtitle";
        }
        //没勾选编号
        if (!showColumns.contains("workcode")) {
            showColumns += ",workcode";
        }
        dataObject.put("showColumns", showColumns);
        params.put("data", dataObject.toJSONString());
        // params.put("Cmd代理cmdkey","参数前写入cmdkey");
        cmd.setParams(params);//数据回写
        Map<String, Object> result = nextExecute(command);//返回值
        //result.put("Cmd代理resultkey","参数后写入result");
        log.writeLog("ExportDailyExcelCmd代理参数结果:" + JSONObject.toJSONString(result));

        try {
            String rootPath = GCONST.getRootPath();
            String status = Convert.toStr(result.get("status"));
            if ("1".equals(status)) {
                String url = Convert.toStr(result.get("url"));
                String readFile = rootPath + url;

                //保存文件路径
                String sourcePath = rootPath + File.separator + "hrm" + File.separator + "kq" + File.separator + "tmpFile" + File.separator;

                //源文件
                String sourceFile = sourcePath + "meir.xlsx";
                //新文件
                String newFile = sourcePath + IdUtil.simpleUUID() + ".xlsx";

                Path source = Paths.get(sourceFile);
                Path destination = Paths.get(newFile);
                //复制文件
                Files.copy(source, destination);


                log.writeLog("readFile:" + readFile);
                log.writeLog("newFile:" + newFile);
                update(readFile, newFile, params);
                //删除原来的文件
                File readfile = new File(readFile);

                readfile.delete();
                File fileTo = new File(newFile);
                fileTo.renameTo(readfile);
                log.writeLog("true");
            }

        } catch (Exception e) {
            log.errorLog("ExportDailyExcelCmd代理异常", e);
        } finally {
            long end = System.currentTimeMillis();
            log.writeLog("耗时: " + (end - start) / 1000.00);
            log.writeLog("ExportDailyExcelCmd代理: end");
        }

        return result;
    }

    public void update(String readFile, String outFile, Map<String, Object> params) throws Exception {
        try (
                FileInputStream fis = new FileInputStream(readFile);
                Workbook workbook = new XSSFWorkbook(fis);
                FileInputStream outStream = new FileInputStream(outFile);
                Workbook outWorkbook = new XSSFWorkbook(outStream);
                FileOutputStream outputStream = new FileOutputStream(outFile);
        ) {
            Sheet sheet = workbook.getSheetAt(0);
            Sheet outSheet = outWorkbook.getSheetAt(0);
            String[] date = getStartDateAndEndDate(params);
            Cell cell = outSheet.getRow(0).getCell(0);
            cell.setCellValue("每日统计 统计日期：" + date[0] + " 至 " + date[1]);
            initStyle(outWorkbook);

            RecordSet rs = new RecordSet();

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
                }else if (cellValue.contains("请假")) {
                    for (int j = 0; j <= 8; j++) {
                        rowIndexMap.put(cellValue + "." + sheet.getRow(rowIndex + 1).getCell(i+j).getStringCellValue(), sheet.getRow(rowIndex + 1).getCell(i+j).getColumnIndex());
                    }

                }else if (cellValue.contains("加班")) {
                    for (int x = 0; x <= 6; x++) {
                        String compensatoryLeave=sheet.getRow(rowIndex + 1).getCell(i+x).getStringCellValue();
                        if (compensatoryLeave.contains("不关联调休")||compensatoryLeave.contains("关联调休")){
                            for (int j = 0; j <= 2; j++) {
                                rowIndexMap.put(cellValue + "."+compensatoryLeave +"."+ sheet.getRow(rowIndex + 2).getCell(i+j).getStringCellValue(), sheet.getRow(rowIndex + 2).getCell(i+j).getColumnIndex());
                            }
                        }
                        rowIndexMap.put(cellValue + "." + sheet.getRow(rowIndex + 1).getCell(i+x).getStringCellValue(), sheet.getRow(rowIndex + 1).getCell(i+x).getColumnIndex());

                    }


                }else {
                    rowIndexMap.put(cellValue, columnIndex);
                }
            }
            log.writeLog("rowIndexMap:"+JSON.toJSONString(rowIndexMap));
            for (int i = 0; i <= sheet.getLastRowNum() - 5; i++) {
                //第5行读取数据
                Row readrRow = sheet.getRow(i + 5);
                //第四行写入
                Row outRow = outSheet.createRow(i + 4);

                Cell cell0 = outRow.createCell(0);//姓名
                cell0.setCellValue(getCellName("姓名", readrRow, rowIndexMap));
                cell0.setCellStyle(cellStyleMap.get("standard"));

                String shiftTimeStr = getCellName("班次", readrRow, rowIndexMap);
                Cell cell1 = outRow.createCell(1);//考勤组
                cell1.setCellValue(shiftTimeStr);
                cell1.setCellStyle(cellStyleMap.get("standard"));
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
                cell2.setCellValue(getCellName("部门", readrRow, rowIndexMap));
                cell2.setCellStyle(cellStyleMap.get("standard"));

                Cell cell3 = outRow.createCell(3);//工号
                cell3.setCellValue(getUserLoginId(getCellName("编号", readrRow, rowIndexMap), rs));
                cell3.setCellStyle(cellStyleMap.get("standard"));

                Cell cell4 = outRow.createCell(4);//职位
                cell4.setCellValue(getCellName("岗位", readrRow, rowIndexMap));
                cell4.setCellStyle(cellStyleMap.get("standard"));


                Cell cell5 = outRow.createCell(5);//日期
                String week = "";
                if (getCellName("日期", readrRow, rowIndexMap).contains("星期")) {
                    week = getCellName("日期", readrRow, rowIndexMap);
                } else {
                    week = getCellName("日期", readrRow, rowIndexMap) + " " + getWeek(getCellName("日期", readrRow, rowIndexMap));
                }
                cell5.setCellValue(week);

                if (!StrUtil.hasEmpty(week) && week.contains("星期六") || week.contains("星期日")) {
                    cell5.setCellStyle(cellStyleMap.get("styleRed"));
                } else {
                    cell5.setCellStyle(cellStyleMap.get("standard"));
                }


                Cell cell6 = outRow.createCell(6);//班次
                cell6.setCellValue(getCellName("班次", readrRow, rowIndexMap));
                cell6.setCellStyle(cellStyleMap.get("standard"));


                Cell cell7 = outRow.createCell(7);//上班1打卡时间
                cell7.setCellValue(getCellName("上班1.打卡时间", readrRow, rowIndexMap));
                cell7.setCellStyle(cellStyleMap.get("standard"));

                Cell cell8 = outRow.createCell(8);//上班1考勤结果
                cell8.setCellValue(getCellName("上班1.考勤结果", readrRow, rowIndexMap));
                getCellStyle(getCellName("上班1.打卡时间", readrRow, rowIndexMap), getCellName("上班1.考勤结果", readrRow, rowIndexMap), cell8);


                Cell cell9 = outRow.createCell(9);//下班1打卡时间
                cell9.setCellValue(getCellName("下班1.打卡时间", readrRow, rowIndexMap));
                cell9.setCellStyle(cellStyleMap.get("standard"));

                Cell cell10 = outRow.createCell(10);//下班1考勤结果
                cell10.setCellValue(getCellName("下班1.考勤结果", readrRow, rowIndexMap));
                getCellStyle(getCellName("下班1.打卡时间", readrRow, rowIndexMap), getCellName("下班1.考勤结果", readrRow, rowIndexMap), cell10);


                Cell cell11 = outRow.createCell(11);//上班2打卡时间
                cell11.setCellValue(getCellName("上班2.打卡时间", readrRow, rowIndexMap));
                cell11.setCellStyle(cellStyleMap.get("standard"));


                Cell cell12 = outRow.createCell(12);//上班2考勤结果
                cell12.setCellValue(getCellName("上班2.考勤结果", readrRow, rowIndexMap));
                getCellStyle(getCellName("上班2.打卡时间", readrRow, rowIndexMap), getCellName("上班2.考勤结果", readrRow, rowIndexMap), cell12);


                Cell cell13 = outRow.createCell(13);//下班2打卡时间
                cell13.setCellValue(getCellName("下班2.打卡时间", readrRow, rowIndexMap));
                cell13.setCellStyle(cellStyleMap.get("standard"));

                Cell cell14 = outRow.createCell(14);//下班2考勤结果
                cell14.setCellValue(getCellName("下班2.考勤结果", readrRow, rowIndexMap));
                getCellStyle(getCellName("下班2.打卡时间", readrRow, rowIndexMap), getCellName("下班2.考勤结果", readrRow, rowIndexMap), cell14);

                Cell cell15 = outRow.createCell(15);//上班3打卡时间
                cell15.setCellValue(getCellName("上班3.打卡时间", readrRow, rowIndexMap));
                cell15.setCellStyle(cellStyleMap.get("standard"));


                Cell cell16 = outRow.createCell(16);//上班3考勤结果
                cell16.setCellValue(getCellName("上班3.考勤结果", readrRow, rowIndexMap));
                getCellStyle(getCellName("上班3.打卡时间", readrRow, rowIndexMap), getCellName("上班3.考勤结果", readrRow, rowIndexMap), cell16);

                Cell cell17 = outRow.createCell(17);//下班3打卡时间
                cell17.setCellValue(getCellName("下班3.打卡时间", readrRow, rowIndexMap));
                cell17.setCellStyle(cellStyleMap.get("standard"));

                Cell cell18 = outRow.createCell(18);//下班3考勤结果
                cell18.setCellValue(getCellName("下班3.考勤结果", readrRow, rowIndexMap));
                getCellStyle(getCellName("下班3.打卡时间", readrRow, rowIndexMap), getCellName("下班3.考勤结果", readrRow, rowIndexMap), cell18);

                Cell cell19 = outRow.createCell(19);//工时---实际工作时长(小时)
                cell19.setCellValue(getCellName("实际工作时长(小时)", readrRow, rowIndexMap));
                cell19.setCellStyle(cellStyleMap.get("standard"));


                Cell cell20 = outRow.createCell(20);//应出勤天数(天)
                cell20.setCellValue(getCellName("应出勤天数(天)", readrRow, rowIndexMap));
                cell20.setCellStyle(cellStyleMap.get("standard"));

                Cell cell21 = outRow.createCell(21);//应工作时长(小时)
                cell21.setCellValue(getCellName("应工作时长(小时)", readrRow, rowIndexMap));
                cell21.setCellStyle(cellStyleMap.get("standard"));

                Cell cell22 = outRow.createCell(22);//实际出勤天数(天)
                cell22.setCellValue(getCellName("实际出勤天数(天)", readrRow, rowIndexMap));
                cell22.setCellStyle(cellStyleMap.get("standard"));

                Cell cell23 = outRow.createCell(23);//实际工作时长(小时)
                cell23.setCellValue(getCellName("实际工作时长(小时)", readrRow, rowIndexMap));
                cell23.setCellStyle(cellStyleMap.get("standard"));

                Cell cell24 = outRow.createCell(24);//迟到(次)
                cell24.setCellValue(getCellName("迟到(次)", readrRow, rowIndexMap));
                cell24.setCellStyle(cellStyleMap.get("standard"));

                Cell cell25 = outRow.createCell(25);//迟到时长(小时)
                cell25.setCellValue(getCellName("迟到时长(小时)", readrRow, rowIndexMap));
                cell25.setCellStyle(cellStyleMap.get("standard"));

                Cell cell26 = outRow.createCell(26);//严重迟到(次)
                cell26.setCellValue(getCellName("严重迟到(次)", readrRow, rowIndexMap));
                cell26.setCellStyle(cellStyleMap.get("standard"));

                Cell cell27 = outRow.createCell(27);//严重迟到时长(小时)
                cell27.setCellValue(getCellName("严重迟到时长(小时)", readrRow, rowIndexMap));
                cell27.setCellStyle(cellStyleMap.get("standard"));

                Cell cell28 = outRow.createCell(28);//早退(次)
                cell28.setCellValue(getCellName("早退(次)", readrRow, rowIndexMap));
                cell28.setCellStyle(cellStyleMap.get("standard"));

                Cell cell29 = outRow.createCell(29);//早退时长(小时)
                cell29.setCellValue(getCellName("早退时长(小时)", readrRow, rowIndexMap));
                cell29.setCellStyle(cellStyleMap.get("standard"));

                Cell cell30 = outRow.createCell(30);//严重早退(次)
                cell30.setCellValue(getCellName("严重早退(次)", readrRow, rowIndexMap));
                cell30.setCellStyle(cellStyleMap.get("standard"));

                Cell cell31 = outRow.createCell(31);//严重早退时长(小时)
                cell31.setCellValue(getCellName("严重早退时长(小时)", readrRow, rowIndexMap));
                cell31.setCellStyle(cellStyleMap.get("standard"));

                Cell cell32 = outRow.createCell(32);//旷工(次)
                cell32.setCellValue(getCellName("旷工(次)", readrRow, rowIndexMap));
                cell32.setCellStyle(cellStyleMap.get("standard"));

                Cell cell33 = outRow.createCell(33);//旷工时长(小时)
                cell33.setCellValue(getCellName("旷工时长(小时)", readrRow, rowIndexMap));
                cell33.setCellStyle(cellStyleMap.get("standard"));

                Cell cell34 = outRow.createCell(34);//漏签(次)
                cell34.setCellValue(getCellName("漏签(次)", readrRow, rowIndexMap));
                cell34.setCellStyle(cellStyleMap.get("standard"));


                Cell cell35 = outRow.createCell(35);//例假(天)
                cell35.setCellValue(getCellName("请假.例假(天)", readrRow, rowIndexMap));
                cell35.setCellStyle(cellStyleMap.get("standard"));

                Cell cell36 = outRow.createCell(36);//事假(小时)
                cell36.setCellValue(getCellName("请假.事假(小时)", readrRow, rowIndexMap));
                cell36.setCellStyle(cellStyleMap.get("standard"));

                Cell cell37 = outRow.createCell(37);//病假(小时)
                cell37.setCellValue(getCellName("请假.病假(小时)", readrRow, rowIndexMap));
                cell37.setCellStyle(cellStyleMap.get("standard"));

                Cell cell38 = outRow.createCell(38);//年假(天)
                cell38.setCellValue(getCellName("请假.年假(天)", readrRow, rowIndexMap));
                cell38.setCellStyle(cellStyleMap.get("standard"));

                Cell cell39 = outRow.createCell(39);//调休(小时)
                cell39.setCellValue(getCellName("请假.调休(小时)", readrRow, rowIndexMap));
                cell39.setCellStyle(cellStyleMap.get("standard"));

                Cell cell40 = outRow.createCell(40);//婚假(天)
                cell40.setCellValue(getCellName("请假.婚假(天)", readrRow, rowIndexMap));
                cell40.setCellStyle(cellStyleMap.get("standard"));

                Cell cell41 = outRow.createCell(41);//产假(天)
                cell41.setCellValue(getCellName("请假.产假(天)", readrRow, rowIndexMap));
                cell41.setCellStyle(cellStyleMap.get("standard"));

                Cell cell42 = outRow.createCell(42);//陪产假(天)
                cell42.setCellValue(getCellName("请假.陪产假(天)", readrRow, rowIndexMap));
                cell42.setCellStyle(cellStyleMap.get("standard"));

                Cell cell43 = outRow.createCell(43);//生产年假(天)
                cell43.setCellValue(getCellName("请假.生产年假(天)", readrRow, rowIndexMap));
                cell43.setCellStyle(cellStyleMap.get("standard"));

                //加班.不关联调休
                Cell cell44 = outRow.createCell(44);//工作日加班(小时)
                cell44.setCellValue(getCellName("加班.不关联调休.工作日加班(小时)", readrRow, rowIndexMap));
                cell44.setCellStyle(cellStyleMap.get("standard"));

                Cell cell45 = outRow.createCell(45);//休息日加班(小时)
                cell45.setCellValue(getCellName("加班.不关联调休.休息日加班(小时)", readrRow, rowIndexMap));
                cell45.setCellStyle(cellStyleMap.get("standard"));

                Cell cell46 = outRow.createCell(46);//节假日加班(小时)
                cell46.setCellValue(getCellName("加班.不关联调休.节假日加班(小时)", readrRow, rowIndexMap));
                cell46.setCellStyle(cellStyleMap.get("standard"));
                //加班.关联调休
                Cell cell47 = outRow.createCell(47);//工作日加班(小时)
                cell47.setCellValue(getCellName("加班.关联调休.工作日加班(小时)", readrRow, rowIndexMap));
                cell47.setCellStyle(cellStyleMap.get("standard"));

                Cell cell48 = outRow.createCell(48);//休息日加班(小时)
                cell48.setCellValue(getCellName("加班.关联调休.休息日加班(小时)", readrRow, rowIndexMap));
                cell48.setCellStyle(cellStyleMap.get("standard"));

                Cell cell49 = outRow.createCell(49);//节假日加班(小时)
                cell49.setCellValue(getCellName("加班.关联调休.节假日加班(小时)", readrRow, rowIndexMap));
                cell49.setCellStyle(cellStyleMap.get("standard"));


                Cell cell50 = outRow.createCell(50);//"总计(小时)"
                cell50.setCellValue(getCellName("加班.总计(小时)", readrRow, rowIndexMap));
                cell50.setCellStyle(cellStyleMap.get("standard"));

                Cell cell51 = outRow.createCell(51);//出差(天)
                cell51.setCellValue(getCellName("出差(天)", readrRow, rowIndexMap));
                cell51.setCellStyle(cellStyleMap.get("standard"));


                Cell cell52 = outRow.createCell(52);//"公出(小时)"
                cell52.setCellValue(getCellName("公出(小时)", readrRow, rowIndexMap));
                cell52.setCellStyle(cellStyleMap.get("standard"));



                Cell cell53 = outRow.createCell(53);//关联的审批单
                String approvalProcess = getApprovalProcess(rs, getCellName("编号", readrRow, rowIndexMap), week, shiftTimeMap, outRow);
                cell53.setCellValue(approvalProcess);
                cell53.setCellStyle(cellStyleMap.get("standard"));


            }
            //excel表单元格创建完成，把存储的样式执行
            if (toBeModifiedStyleMap.size() > 0) {
                for (Integer integerKey : toBeModifiedStyleMap.keySet()) {
                    Row row = outSheet.getRow(integerKey);
                    List<Integer> cellList = toBeModifiedStyleMap.get(integerKey);
                    int[] cellArray = cellList.stream().mapToInt(Integer::intValue).toArray();
                    CellStyle cellStyle = cellStyleMap.get("TakeTimeOffWorkToGoOut");
                    setCellStyle(row, cellArray, cellStyle);
                }

            }
            outWorkbook.write(outputStream);
            System.out.println("更新完成");
        } catch (Exception e) {
            throw e;
        }

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

    //根据日期获取考勤流程日期跟时间
    private String getApprovalProcess(RecordSet rs, String loginid, String date, Map<String, Integer> shiftTimeMap, Row row) {

        StringBuffer approvalStr = new StringBuffer();

        if (workflow.size() == 0) {
            //查询考勤类流程
            rs.executeQuery("select id,workflowname,workflowdesc,formid from workflow_base  where  workflowtype = 7");
            while (rs.next()) {
                Map<String, String> work = new HashMap<>();
                work.put("formid", Math.abs(rs.getInt("formid")) + "");
                work.put("id", rs.getString("id"));
                work.put("workflowname", rs.getString("workflowname"));
                workflow.add(work);
            }
        }
        User user = userInfo.get(loginid);
        if (user == null || StrUtil.hasEmpty(date)) {
            return approvalStr.toString();
        }
        date = date.substring(0, 10).trim();

        String workIds = "";
        for (Map<String, String> workflow : workflow) {
            workIds += workflow.get("id") + ",";
        }
        if (StrUtil.hasEmpty(workIds)) {
            return approvalStr.toString();
        }
        //去掉最后一位逗号
        workIds = workIds.substring(0, workIds.length() - 1);
        if (currentRequestIdsMap.get(user.getUID()) == null) {
            List<String> ids = new ArrayList<>();
            rs.executeQuery("select requestid from workflow_requestbase where workflowid in (" + workIds + ") and creater=? and currentnodetype=3", user.getUID());
            while (rs.next()) {
                ids.add(rs.getString("requestid"));
            }
            currentRequestIdsMap.put(user.getUID(), ids);
        }
        //获取缓存是否为空
        if (currentRequestIdsMap.get(user.getUID()) == null || currentRequestIdsMap.get(user.getUID()).size() == 0) {
            return approvalStr.toString();
        }

        List<String> requestIdList = currentRequestIdsMap.get(user.getUID());

        for (String requestId : requestIdList) {
            String workflowname = "";
            int formId = 0;
            String workflowId = "";
            rs.executeQuery("select w.workflowname,w.formid,w.id from workflow_requestbase r left join workflow_base w on r.workflowid=w.id where r.requestid=?", requestId);
            if (rs.next()) {
                workflowname = rs.getString("workflowname");
                formId = Math.abs(rs.getInt("formid"));
                workflowId = rs.getString("id");
            }
            //补卡
            if (workflowname.contains("补卡")) {
                rs.executeQuery("select d.bkrq,d.bksj from formtable_main_" + formId + " m left join formtable_main_" + formId + "_dt1 d on m.id=d.mainid  where m.requestid=? and d.bkrq=?", requestId, date);
                while (rs.next()) {
                    approvalStr.append("补卡 " + rs.getString("bkrq") + " " + rs.getString("bksj") + "\n");
                    Integer bksj = shiftTimeMap.get(rs.getString("bksj"));
                    if (bksj != null) {
                        addStyle(row.getRowNum(),bksj);
                    }
                    addStyle(row.getRowNum(),53);
                }
            }
            //出差流程
            if (workflowname.contains("出差")) {
                //出差流程
                String kssj = "";
                String jssj = "";
                String ccts = "";
                rs.executeQuery("select kssj,jssj,kssj1,jssj1,ccts from formtable_main_" + formId + " m left join  formtable_main_" + formId + "_dt1 d on m.id=d.mainid where requestid=? and kssj=?", requestId, date);
                while (rs.next()) {
                    if (StrUtil.hasEmpty(kssj)) {
                        kssj = rs.getString("kssj") + " " + rs.getString("kssj1");
                    }
                    jssj = rs.getString("jssj") + " " + rs.getString("jssj1");
                    ccts = rs.getString("ccts");
                }
                if (!StrUtil.hasEmpty(kssj, kssj, ccts)) {
                    approvalStr.append(workflowname + " " + kssj + "到" + jssj + "  " + ccts + "\n");
                }

            }

            //外出流程
            if (workflowname.contains("外出")) {
                //缓存外出类型
                if (outOfOfficeType.size() == 0) {
                    rs.executeQuery("select selectvalue,selectname from workflow_SelectItem where FIELDID=(select id  from (select id, fieldname, fieldlabel, viewtype, detailtable from workflow_billfield  where billid = (select formid from workflow_base where id= ?) and fieldname= ? ) a   left join htmllabelinfo b on a.fieldlabel = b.indexid and languageid = 7)", workflowId, "wclx");
                    while (rs.next()) {
                        outOfOfficeType.put(rs.getString("selectvalue"), rs.getString("selectname"));

                    }
                }
                rs.executeQuery("select requestid,ksrq,kssjnew,jsrq,jssjnew,wcsjxs,wclx from formtable_main_" + formId + " where requestid=? and ksrq=?", requestId, date);
                if (rs.next()) {
                    String dateTime1 = rs.getString("ksrq") + " " + rs.getString("kssjnew");
                    String dateTime2 = rs.getString("jsrq") + " " + rs.getString("jssjnew");
                    approvalStr.append(outOfOfficeType.get(rs.getString("wclx")) + " " + dateTime1 + "到" + dateTime2 + " " + rs.getString("wcsjxs") + "\n");
                    addStyle(row.getRowNum(),53);
                    setCellStyleList(dateTime1, dateTime2, shiftTimeMap, row);

                }
            }
            //加班流程
            if (workflowname.contains("加班")) {
                rs.executeQuery("select jbksrq,jbkssj,jbjsrq,jbjssj,scxs from formtable_main_" + formId + " where requestid=? and jbksrq=?", requestId, date);
                if (rs.next()) {
                    approvalStr.append(workflowname + " " + rs.getString("jbksrq") + " " + rs.getString("jbkssj") + "到" + rs.getString("jbjsrq") + " " + rs.getString("jbjssj") + " " + rs.getString("scxs") + "\n");
                }
            }

            //请假流程
            if (workflowname.contains("请假")) {
                //缓存请假类型
                if (typeOfLeave.size() == 0) {
                    rs.executeQuery("select id,leaveName  from kq_LeaveRules  where (isDelete is null or isDelete!=1) order by showOrder,id  asc");
                    while (rs.next()) {
                        typeOfLeave.put(rs.getString("id"), rs.getString("leaveName"));
                    }
                }
                rs.executeQuery("select ksrq,kssj,jsrq,jssj,qjts,qjlx from formtable_main_" + formId + " where requestid=? and ksrq=?", requestId, date);
                if (rs.next()) {
                    String dateTime1 = rs.getString("ksrq") + " " + rs.getString("kssj");
                    String dateTime2 = rs.getString("jsrq") + " " + rs.getString("jssj");
                    approvalStr.append(typeOfLeave.get(rs.getString("qjlx")) + " " + dateTime1 + "到" + dateTime2 + " " + rs.getString("qjts") + "\n");
                    addStyle(row.getRowNum(),53);
                    setCellStyleList(dateTime1, dateTime2, shiftTimeMap, row);
                }
            }
        }


        // 获取考勤流程sql
        return approvalStr.toString();
    }

    /**
     * 判断给定时间是否包含在开始时间和结束时间之内
     *
     * @param startTime   开始时间
     * @param endTime     结束时间
     * @param timeToCheck 给定时间
     * @return
     */
    public static boolean isTimeBetweenStartAndEndTime(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime timeToCheck) {
        return !timeToCheck.isBefore(startTime) && !timeToCheck.isAfter(endTime);
    }

    /**
     * 设置考勤结果背景色
     *
     * @param dateTime1    请假开始时间  yyyy-MM-dd HH:mm
     * @param dateTime2    请假结束时间  yyyy-MM-dd HH:mm
     * @param shiftTimeMap 班次
     * @param row          当前行
     */
    public void setCellStyleList(String dateTime1, String dateTime2, Map<String, Integer> shiftTimeMap, Row row) {
        if (StrUtil.hasEmpty(dateTime1, dateTime2)) {
            return;
        }
        Date dateToDate1 = DateUtil.parse(dateTime1);
        Date dateToDate2 = DateUtil.parse(dateTime2);
        long betweenDay = DateUtil.between(dateToDate1, dateToDate2, DateUnit.HOUR);
        //往前获取索引
        LocalDateTime startDateTime = LocalDateTime.parse(dateTime1, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        LocalDateTime endDateTime = LocalDateTime.parse(dateTime2, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        int divisor = 24;//一天24小时
        long quotient = betweenDay / divisor;
        long remainder = betweenDay % divisor;
        //不足24小时加1
        if (remainder > 0) {
            quotient++;
        }



        for (int i = 0; i <= quotient; i++) {
            //获取当前行日期
            String date = row.getCell(5).getStringCellValue();
            if (StrUtil.hasEmpty(date)) {
                continue;
            }
            int  currentRowIndex=row.getRowNum() + i;
            date = date.substring(0, 10).trim();
            Date dateToDate = DateUtil.parse(date);
            //时间偏移单位天
            date = DateUtil.offset(dateToDate, DateField.DAY_OF_MONTH, i).toString().substring(0, 10).trim();
            for (String key : shiftTimeMap.keySet()) {
                LocalDateTime currentDateTime = LocalDateTime.parse(date + " " + key, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                //存样式，等单元格创建完成后再新增样式
                if (isTimeBetweenStartAndEndTime(startDateTime, endDateTime, currentDateTime)) {
                    addStyle(currentRowIndex,shiftTimeMap.get(key));

                }
            }


        }

    }

    public void addStyle(int currentRowIndex,int cellIndex ){
        if (toBeModifiedStyleMap.get(currentRowIndex)==null){
            List<Integer> cellList=new ArrayList<>();
            cellList.add(cellIndex);
            toBeModifiedStyleMap.put(currentRowIndex,cellList);
        }else {
            List<Integer> cellList = toBeModifiedStyleMap.get(currentRowIndex);
            cellList.add(cellIndex);
            toBeModifiedStyleMap.put(currentRowIndex,cellList);
        }
    }

    /**
     * 设置样式
     *
     * @param row       当前行
     * @param indexs    要设置的单元格
     * @param cellStyle 要设置的样式
     */
    private void setCellStyle(Row row, int[] indexs, CellStyle cellStyle) {
        for (int i = 0; i < indexs.length; i++) {
            if (row==null){
                continue;
            }
            Cell cell = row.getCell(indexs[i]);
            if (cell!=null&&!StrUtil.hasEmpty(cell.getStringCellValue())) {
                cell.setCellStyle(cellStyle);
            }
        }

    }

    /**
     * 根据名字获取工号
     *
     * @param rs
     * @return
     */
    private String getUserLoginId(String loginid, RecordSet rs) {
        User user = userInfo.get(loginid);
        if (user != null) {
            return user.getLoginid();
        }
        rs.executeQuery("select id from hrmResource where loginid=?", loginid);
        if (rs.next()) {
            userInfo.put(loginid, new User(rs.getInt("id")));
            return userInfo.get(loginid).getLoginid();
        }
        return "";
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

    /**
     * 获取筛选的时间
     *
     * @param parms
     * @return
     */
    public String[] getStartDateAndEndDate(Map<String, Object> parms) {
        String data = Util.null2String(parms.get("data"));
        JSONObject dataObject = JSON.parseObject(data);
        String typeselect = Util.null2String(dataObject.get("typeselect"));
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
        return new String[]{startDateTime, endDateTime};
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

//    public static void main(String[] args) throws Exception {
//        ExportDailyExcelCmdProxy proxy = new ExportDailyExcelCmdProxy();
//        String readFile = "F:\\code\\changfeng\\xlsx\\a3c60c82-e2c0-4b23-b12e-072c43eff6b5(20230414135849).xlsx";
//        String outFile = "F:\\code\\changfeng\\xlsx\\meir.xlsx";
//        proxy.update(readFile, outFile, new HashMap<>());
//    }


}
