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
import com.engine.kq.cmd.report.GetKQDailyReportCmd;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.TimeUtil;
import weaver.general.Util;
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
import java.util.stream.Collectors;


/**
 * @author Li Yu Feng
 * @date 2022-08-21 11:04
 */
@CommandDynamicProxy(target = GetKQDailyReportCmd.class, desc = "每日统计报表列表数据二次开发")
public class GetKQDailyReportCmdProxy extends AbstractCommandProxy<Map<String, Object>> {
    private BaseBean log = new BaseBean();
    private List<Map<String, String>> workflow = new ArrayList<>();
    //存当前人的流程id,避免重复查询数据库
    private Map<Integer, List<String>> currentRequestIdsMap = new HashMap<>();

    //缓存外出类型
    private Map<String, String> outOfOfficeType = new HashMap<>();
    //存储请假类型
    Map<String, String> typeOfLeave = new HashMap<>();
    //存储颜色
    Map<String,List<String>> stylesMap=new HashMap<>();


    @Override
    public Map<String, Object> execute(Command<Map<String, Object>> command) {
        log.writeLog("GetKQDailyReportCmd代理: start");
        long start = System.currentTimeMillis();
        GetKQDailyReportCmd cmd = (GetKQDailyReportCmd) command;//获取代理对象
        Map<String, Object> params = cmd.getParams();//代理前的参数
        log.writeLog("GetKQDailyReportCmd代理参数前:" + JSONObject.toJSONString(params));
        // params.put("Cmd代理cmdkey","参数前写入cmdkey");
        cmd.setParams(params);//数据回写
        Map<String, Object> result = nextExecute(command);//返回值


        //result.put("Cmd代理resultkey","参数后写入result");
        log.writeLog("GetKQDailyReportCmd代理参数结果:" + JSONObject.toJSONString(result));

        try {
            //要去掉的列
            //String[] attr=new String[]{"workdays", "workmins","attendancedays","beLate","beLateMins","graveBeLate","graveBeLateMins","leaveEearly","leaveEarlyMins","graveLeaveEarly","graveLeaveEarlyMins","absenteeism","absenteeismMins","forgotCheck","leave","overtime","businessLeave","officialBusiness"};
            //List<String> columnList = Arrays.asList(attr);

            List<Map<String,Object>> columns =(List<Map<String,Object>>) result.get("columns");
            // 使用 Stream 进行过滤
//            List<Map<String,Object>> filteredColumns = columns.stream()
//                    .filter(column -> !columnList.contains(Util.null2String(column.get("dataIndex"))))
//                    .collect(Collectors.toList());
            Map<String,Object> approvalSlipMap=new HashMap<>();
            //{dataIndex:'approvalSlip',key:'approvalSlip',rowSpan:3,showDetial:'1',title:'关联的审批单',unit:'',width:105}
            approvalSlipMap.put("dataIndex","approvalSlip");
            approvalSlipMap.put("key","approvalSlip");
            approvalSlipMap.put("rowSpan",3);
            approvalSlipMap.put("showDetial","1");
            approvalSlipMap.put("title","关联的审批单");
            approvalSlipMap.put("unit","");
            approvalSlipMap.put("width",500);
            columns.add(approvalSlipMap);

            result.put("columns",columns);
            RecordSet rs = new RecordSet();

            List<Map<String,Object>> datas =(List<Map<String,Object>>) result.get("datas");



            datas.forEach(data -> data.put("approvalSlip", getApprovalProcess(rs,Util.null2String(data.get("resourceId")),Util.null2String(data.get("kqdate")),data)));
            if(stylesMap.size()>0){
                datas.forEach(data ->{
                    String kqdate = Util.null2String(data.get("kqdate"));
                    List<String> styles = stylesMap.get(kqdate);
                    if (styles!=null&&styles.size()>0){
                        data.put("style",styles);
                    }
                });

            }

            result.put("datas",datas);


        } catch (Exception e) {
            log.errorLog("GetKQDailyReportCmdProxy代理异常", e);
        } finally {
            long end = System.currentTimeMillis();
            log.writeLog("耗时: " + (end - start) / 1000.00);
            log.writeLog("GetKQDailyReportCmdProxy代理: end");
        }
        return result;
    }



    private String getApprovalProcess(RecordSet rs, String resourceId, String date,Map<String,Object> data) {
        String serialid = Util.null2String(data.get("serialid"));
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
        User user = new User(Integer.valueOf(resourceId));
        if (user == null || StrUtil.hasEmpty(date)) {
            return approvalStr.toString();
        }
        date = date.substring(0, 10).trim();
        //signinstatus1  上班考勤结果
        //signintime1    上班时间
        //signoutstatus1  下班考勤结果
        //signouttime1   下班时间
        //设置打卡时间结果样式
        initCellStyle(data,"signintime","signinstatus");
        initCellStyle(data,"signouttime","signoutstatus");
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
                    String bksj = shiftTimeMap.get(rs.getString("bksj"));
                    if (bksj != null) {
                        addStyle(date,bksj,"TakeTimeOffWorkToGoOut");
                    }
                    addStyle(date,"approvalSlip","TakeTimeOffWorkToGoOut");
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
                    addStyle(date,"approvalSlip","TakeTimeOffWorkToGoOut");
                    setCellStyleList(dateTime1, dateTime2, shiftTimeMap, date);

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
                    addStyle(date,"approvalSlip","TakeTimeOffWorkToGoOut");
                    setCellStyleList(dateTime1, dateTime2, shiftTimeMap, date);
                }
            }
        }
        return approvalStr.toString();


    }

    /**
     * 根据请假时间设置样式
     * @param dateTime1
     * @param dateTime2
     * @param shiftTimeMap
     * @param date
     */
    public void setCellStyleList(String dateTime1, String dateTime2, Map<String, String> shiftTimeMap, String date) {
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
                    addStyle(date,shiftTimeMap.get(key),"TakeTimeOffWorkToGoOut");

                }
            }
            //循环结束需要重置回来原来的日期
            date=bakDate;


        }


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
     * 根据日期存储颜色
     * @param date
     * @param field
     * @param color
     */
    public void  addStyle(String date,String field,String color){
        List<String> styleList = stylesMap.get(date);
        if(styleList!=null&&styleList.size()>0){
            if (!styleList.contains(color)){
                styleList.add(color);
            }
            styleList.add(field);
            stylesMap.put(date,styleList);
        }else {
            styleList=new ArrayList<>();
            styleList.add(field);
            styleList.add(color);
            stylesMap.put(date,styleList);
        }
    }
    //设置打卡结果样式
    public void initCellStyle(Map<String,Object> data,String time,String status) {
        //1.缺卡   未打卡&&旷工  CORAL
        //2.迟到   无&&迟到   BRIGHT_GREEN
        //3.早退   无&&早退   LIGHT_YELLOW
        //4.补卡审批通过
        for (int i = 1; i <=3; i++) {
            //signinstatus1  上班考勤结果
            //signintime1    上班时间
            //signoutstatus1  下班考勤结果
            //signouttime1   下班时间


            String clockTimeKey=time+i;
            String clockOutcomeKey=status+i;
            //1.缺卡   未打卡&&旷工  CORAL
            String clockTime = Util.null2String(data.get(clockTimeKey));
            String clockOutcome = Util.null2String(data.get(clockOutcomeKey));
            String kqdate = Util.null2String(data.get("kqdate"));

            if (!StrUtil.hasEmpty(clockTime) && clockTime.equals("未打卡") && clockOutcome.contains("旷工")) {
                addStyle(kqdate,clockOutcomeKey,"CORAL");
            } else if (clockOutcome.contains("迟到")) {
                addStyle(kqdate,clockOutcomeKey,"BRIGHT_GREEN");
            } else if (clockOutcome.contains("早退")) {
                addStyle(kqdate,clockOutcomeKey,"LIGHT_YELLOW");
            } else if (clockOutcome.contains("漏签")) {
                addStyle(kqdate,clockOutcomeKey,"CORAL");
            }

        }

    }
}
