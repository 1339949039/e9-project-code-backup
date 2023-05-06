package com.action.processpush;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.weaver.general.Util;
import weaver.file.Prop;
import weaver.general.BaseBean;
import weaver.ofs.interfaces.SendRequestStatusDataInterfaces;
import weaver.workflow.request.todo.DataObj;
import weaver.workflow.request.todo.RequestStatusObj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * 推送流程到统建系统
 *
 * @author
 * @date 2022-09-07 09:59
 */
public class ProcessPushAction implements SendRequestStatusDataInterfaces {
    private BaseBean log = new BaseBean();

    //推送code
    String pushSysCode = Util.null2String(Prop.getPropValue("AutoLogin", "pushSysCode"));
    /**
     * 后台设置id
     */
    public String id;
    /**
     * 设置的系统编号
     */
    public String syscode;
    /**
     * 服务器URL
     */
    public String serverurl;
    /**
     * 流程白名单
     */
    public ArrayList<String> workflowwhitelist;
    /**
     * 人员白名单
     */
    public ArrayList<String> userwhitelist;


    public String getId() {
        return id;
    }

    public String getSyscode() {
        return syscode;
    }

    public String getServerurl() {
        return serverurl;
    }

    public ArrayList<String> getWorkflowwhitelist() {
        return workflowwhitelist;
    }

    public ArrayList<String> getUserwhitelist() {
        return userwhitelist;
    }

    private boolean isTab = true;

    /**
     * 实现消息推送的具体方法
     *
     * @param datas 传入的请求数据对象数据集
     */
    public void SendRequestStatusData(ArrayList<DataObj> datas) {
        log.writeLog("统建系统集成推送流程到统建系统: Start");
        try {
            log.writeLog("id:" + id);
            log.writeLog("syscode:" + syscode);
            log.writeLog("serverurl:" + serverurl);
            log.writeLog("workflowwhitelist:" + JSONUtil.toJsonStr(workflowwhitelist));
            log.writeLog("userwhitelist:" + JSONUtil.toJsonStr(userwhitelist));
            log.writeLog("datas:" + JSONUtil.toJsonStr(datas));
            for (DataObj dobj : datas) {
                ArrayList<RequestStatusObj> dobjTododatas = dobj.getTododatas();//待办
                log.writeLog("dobjTododatas:" + JSONUtil.toJsonStr(dobjTododatas));
                //发送待办
                if (dobjTododatas.size() > 0) {
                    sendRequest(dobjTododatas);
                }
                ArrayList<RequestStatusObj> dobjDonedatas = dobj.getDonedatas();//已办
                log.writeLog("dobjDonedatas:" + JSONUtil.toJsonStr(dobjDonedatas));
                //发送已办
                if (dobjDonedatas.size() > 0) {
                    sendRequest(dobjDonedatas);
                }
                String requestid = dobj.getRequestid();
                log.writeLog("requestid:" + requestid);

                String sendtimestamp = dobj.getSendtimestamp();
                log.writeLog("sendtimestamp:" + sendtimestamp);
            }
        } catch (Exception e) {
            log.errorLog("推送统建系统异常:", e);
        }
        log.writeLog("统建系统集成推送流程到统建系统: end");
    }

    /**
     * /rest/ofs/ReceiveRequestInfoByMap
     * 发送待办，已办，办结
     * @param processDatas
     * @return
     */
    public void sendRequest(ArrayList<RequestStatusObj> processDatas) throws Exception {
        String timestamp = System.currentTimeMillis() + "";//时间戳
        for (RequestStatusObj dataObj : processDatas) {
            Map<String, Object> data = new HashMap<>();
            data.put("syscode", pushSysCode);
            data.put("flowid", dataObj.getRequestid());//唯一id
            data.put("requestname", dataObj.getRequstname());//流程标题
            data.put("workflowname", dataObj.getWorkflowname());//流程名称
            data.put("nodename", dataObj.getNodename());//流程节点名称
            data.put("pcurl", "/spa/workflow/static4form/index.html?_rdm=" + timestamp + "#/main/workflow/req?requestid=" + dataObj.getRequestid() + "&preloadkey=" + timestamp + "&timestamp=" + timestamp);
            data.put("appurl", "/spa/workflow/static4mobileform/index.html?_random=" + timestamp + "#/req?requestid=" + dataObj.getRequestid() + "&f_weaver_belongto_userid=1&f_weaver_belongto_usertype=&timestamp=" + timestamp);//移动端url
            data.put("creator", dataObj.getCreator().getLoginid());//创建人
            data.put("createdatetime", dataObj.getCreatedate() + " " + dataObj.getCreatetime());//创建日期时间
            data.put("receiver", dataObj.getUser().getLoginid());//接收人
            data.put("receivedatetime", DateUtil.now());//接收日期时间
            data.put("isremark", dataObj.getIsremark());// 0 待办 2：已办 4：办结
            String viewType=dataObj.getViewtype();
            if(dataObj.getIsremark().equals("2")){
                viewType="1";
            }
            data.put("viewtype", viewType);//流程查看状态 0：未读 1：已读;
            data.put("receivets", timestamp);//当前时间戳
            String result = HttpRequest.post(serverurl)
                    .form(data)
                    .execute().body();
            log.writeLog("创建人:" + dataObj.getCreator().getLoginid() + "   流程名称:" + dataObj.getRequstname() + "   流程ID:" + dataObj.getRequestid() + "   接收人:" + dataObj.getUser().getLoginid() + "   推送结果:" + result);
        }
    }

}