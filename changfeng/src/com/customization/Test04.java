package com.customization;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import weaver.general.Util;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Li Yu Feng
 * @date 2023-04-17 17:20
 */
public class Test04 {
    Map<String,Integer> shiftTimeMap=new HashMap<>();
    public List<Integer> getCellIndex(String dateTime1, String dateTime2) {
        List<Integer> index = new ArrayList<>();
        if (StrUtil.hasEmpty(dateTime1, dateTime2)) {
            return index;
        }
        Date dateToDate1 = DateUtil.parse(dateTime1);
        Date dateToDate2 = DateUtil.parse(dateTime2);
        long betweenDay = DateUtil.between(dateToDate1, dateToDate2, DateUnit.DAY);
        //往前获取索引
        LocalDateTime startDateTime = LocalDateTime.parse(dateTime1, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        LocalDateTime endDateTime = LocalDateTime.parse(dateTime2, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        //是跨天请假
        if (betweenDay > 0) {

        } else {
            for (String key : shiftTimeMap.keySet()) {

            }
        }
        return index;
    }

    /**
     * 判断给定时间是否包含在开始时间和结束时间之内
     * @param startTime 开始时间
     * @param endTime  结束时间
     * @param timeToCheck 给定时间
     * @return
     */
    public static boolean isTimeBetweenStartAndEndTime(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime timeToCheck) {
        return !timeToCheck.isBefore(startTime) && !timeToCheck.isAfter(endTime);
    }

    public static void main(String[] args) {
        /*String startDateString = "2023-04-10 17:30";
        String endDateString = "2023-04-20 17:30";

        LocalDateTime startDateTime = LocalDateTime.parse(startDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        LocalDateTime endDateTime = LocalDateTime.parse(endDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        LocalDateTime dateTime = LocalDateTime.parse("2023-04-20 17:30", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        Test04 test04 = new Test04();
        System.out.println("是否包含 = " + test04.isTimeBetweenStartAndEndTime(startDateTime, endDateTime, dateTime));*/
        String dateStr = "2023-04-21";
        Date date = DateUtil.parse(dateStr);

        Date newDate = DateUtil.offset(date, DateField.DAY_OF_MONTH, 0);
        System.out.println("newDate.toString() = " + newDate.toString());

    }

}
