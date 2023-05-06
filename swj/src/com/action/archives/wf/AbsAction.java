package com.action.archives.wf;

import org.apache.commons.lang.StringUtils;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.*;
import weaver.workflow.request.RequestManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbsAction implements Action {

    /**
     * 将requestInfo 对象中主表对象MainTableInfo转换成Map对象
     */
    protected Map<String, String> getMainTableMap(RequestInfo requestinfo) {
        MainTableInfo maintable = requestinfo.getMainTableInfo();
        Property[] propertys = maintable.getProperty();
        Map<String, String> map = new HashMap<String, String>();
        for (Property pro : propertys) {
            String name = pro.getName();
            String value = Util.null2String(pro.getValue());
            map.put(name, value);
        }
        return map;
    }

    /**
     * 获得流程表单指定明细表的数据信息
     *
     * @param requestinfo
     * @param mxindex     0表示明细表1
     * @return
     */
    protected List<Map<String, String>> getMXTableListMap(
            RequestInfo requestinfo, int mxindex) {
        // 封装明细表字段
        DetailTable[] detailtable = requestinfo.getDetailTableInfo().getDetailTable();// 获取所有明细表
        List<Map<String, String>> lists = new ArrayList<Map<String, String>>();
        if (detailtable.length > 0) {
            DetailTable dt = detailtable[mxindex];// 指定明细表
            Row[] s = dt.getRow();// 当前明细表的所有数据,按行存储
            for (int j = 0; j < s.length; j++) {
                Map<String, String> map = new HashMap<String, String>();
                Row r = s[j];// 指定行
                String id = r.getId();
                for (Cell cell : r.getCell()) {// 每行数据再按列存储
                    String name = cell.getName();// 明细字段名称
                    String value = cell.getValue();// 明细字段的值
                    map.put(name, value);
                }
                map.put("mxid", id);
                lists.add(map);
            }
        }
        return lists;
    }

    /**
     * 去null
     *
     * @param detailtableMap
     * @return
     * @throws Exception
     */
    protected Map<String, List<Map<String, String>>> getDetailtableMap(Map<String, List<Map<String, String>>> detailtableMap) {

        Map<String, List<Map<String, String>>> returnDetailtableMap = new HashMap<>();

        if (detailtableMap != null && detailtableMap.size() > 0) {

            for (Map.Entry<String, List<Map<String, String>>> entry : detailtableMap.entrySet()) {

                List<Map<String, String>> detailtableList = entry.getValue();
                List<Map<String, String>> list = new ArrayList<>();

                if (detailtableList != null && detailtableList.size() > 0) {

                    for (int i = 0; i < detailtableList.size(); i++) {
                        Map<String, String> map = detailtableList.get(i);
                        if (map == null) {
                            map = new HashMap<>();
                        } else {
                            map = detailtableList.get(i);
                        }
                        list.add(map);
                    }

                }
                returnDetailtableMap.put(entry.getKey(), list);
            }

        }
        return returnDetailtableMap;
    }

    /**
     * 错误信息反馈
     */
    protected String returnErrorMes(String mes, RequestManager requestManager) {
        requestManager.setMessage("1");
        requestManager.setMessagecontent("流程提交失败:" + mes);
        return FAILURE_AND_CONTINUE;
    }

    protected String doResult(String actionStatus, String msg, RequestManager requestManager) {

        if (StringUtils.equals(actionStatus, Action.SUCCESS)) {
            return Action.SUCCESS;
        } else {
            return returnErrorMes(msg, requestManager);
        }
    }
}
