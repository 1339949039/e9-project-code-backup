package com.action.kinggdee;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.action.kinggdee.wf.AbsAction;
import com.action.kinggdee.wf.ActionResult;
import com.google.gson.Gson;
import com.kingdee.bos.webapi.entity.RepoRet;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import kingdee.bos.webapi.client.K3CloudApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weaver.hrm.User;
import weaver.soa.workflow.request.RequestInfo;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * @author Li Yu Feng
 * @date 2023-03-02 14:28
 */
public class KingdeeCredentialIntegrationAction extends AbsAction {
    // 获取自定义的 logger,
    Logger logger = LoggerFactory.getLogger("lyflog");
    private String actionMark = "";

    public KingdeeCredentialIntegrationAction() {
    }

    public String getActionMark() {
        return this.actionMark;
    }

    @Override
    public String execute(RequestInfo requestInfo) {
        long start = System.currentTimeMillis();
        logger.info("金蝶凭证集成: start");
        String requestid = requestInfo.getRequestid();
        logger.info("requestid = " + requestid);
        User user = new User(requestInfo.getRequestManager().getCreater());//流程创建用户
        logger.info("用户信息 = " + JSONUtil.toJsonStr(user));
        Map<String, String> mainTableMap = this.getMainTableMap(requestInfo);//主表数据
        logger.info("主表数据 = " + JSONUtil.toJsonStr(mainTableMap));
        ActionResult actionResult = new ActionResult();
        String requestName = requestInfo.getRequestManager().getRequestname();
        logger.info("requestName = " + requestName);
        String wfTablename = requestInfo.getRequestManager().getBillTableName();
        logger.info("wfTablename = " + wfTablename);

        try {
            StringBuffer buffer = new StringBuffer();
            //基本信息，固定不变
            buffer.append("{\"NeedUpDateFields\":[],\"NeedReturnFields\":[],\"IsDeleteEntry\":true,\"SubSystemId\":\"GL\",\"IsVerifyBaseDataField\":true,\"IsEntryBatchFill\":false,\"ValidateFlag\":true,\"NumberSearch\":true,\"IsAutoAdjustField\":false,\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\",\"IsControlPrecision\":false,\"ValidateRepeatJson\":false,");
            String zbid = isNullString(mainTableMap.get("zbid"));
            String sqrq = isNullString(mainTableMap.get("sqrq"));
            String pzz = isNullString(mainTableMap.get("pzz"));

            //账簿信息
            buffer.append(" \"Model\":{\"FAccountBookID\":{\"FNumber\":\"" + zbid + "\"},\"FDate\":\"" + sqrq + "\",\"FVOUCHERGROUPID\":{\"FNumber\":\"" + pzz + "\"},\"FEntity\":[");
            int mxindex1 = 3;//获取第4个明细表数据 数组从0开始
            int mxindex2 = 4;//获取第5个明细表数据 数组从0开始
            if (requestName.contains("对公支付流程")) {
                mxindex1 = 1;//对公支付流程是取的第二个明细表
                mxindex2 = 2;//对公支付流程是取的第三个明细表
            }
            List<Map<String, String>> detail4 = this.getMXTableListMap(requestInfo, mxindex1);
            logger.info("detail4 = " + JSONUtil.toJsonStr(detail4));

            String pzzy = isNullString(mainTableMap.get("pzzy"));
            for (Map<String, String> item : detail4) {
                String kmbm = isNullString(item.get("kmbm"));
                String jfje = isNullString(item.get("jfje"));
                //借方信息
                buffer.append("{\"FEXPLANATION\":\"" + pzzy + "\",\"FACCOUNTID\":{\"FNumber\":\"" + kmbm + "\"},\"FCURRENCYID\":{\"FNumber\":\"PRE001\"},\"FEXCHANGERATETYPE\":{\"FNumber\":\"HLTX01_SYS\"},\"FEXCHANGERATE\":\"1\",\"FAMOUNTFOR\":\"" + jfje + "\",\"FDEBIT\":\"" + jfje + "\"},");
            }
            List<Map<String, String>> detail5 = this.getMXTableListMap(requestInfo, mxindex2);
            logger.info("detail5 = " + JSONUtil.toJsonStr(detail5));

            for (Map<String, String> item : detail5) {
                String kmbm = isNullString(item.get("kmbm"));
                String dfje = isNullString(item.get("dfje"));
                //贷方信息
                buffer.append(" {\"FEXPLANATION\":\"" + pzzy + "\",\"FACCOUNTID\":{\"FNumber\":\"" + kmbm + "\"},\"FCURRENCYID\":{\"FNumber\":\"PRE001\"},\"FEXCHANGERATETYPE\":{\"FNumber\":\"HLTX01_SYS\"},\"FEXCHANGERATE\":\"1\",\"FAMOUNTFOR\":\"" + dfje + "\",\"FCREDIT\":\"" + dfje + "\"},");
            }
            //尾部信息
            buffer.append("]}}");
            logger.info("拼接buffer后结果 = " + buffer.toString());
            buffer.deleteCharAt(buffer.length() - 4);//删除最后的逗号
            logger.info("拼接buffer删除逗号后结果 = " + buffer.toString());
            //业务对象标识
            String formId = "GL_VOUCHER";
            K3CloudApi api = new K3CloudApi();
            //调用接口
            String resultJson = api.save(formId, buffer.toString());
            logger.info("resultJson结果 = " + resultJson);
            if (StrUtil.hasEmpty(resultJson)) {
                throw new Exception("请求结果为空");
            }
            JSONObject object = JSONUtil.parseObj(resultJson);
            Object ResultStr = object.get("Result");
            JSONObject ResultObj = JSONUtil.parseObj(ResultStr);
            Object ResponseStatusStr = ResultObj.get("ResponseStatus");
            JSONObject ResponseStatusObj = JSONUtil.parseObj(ResponseStatusStr);
            Object success = ResponseStatusObj.get("IsSuccess");
            Boolean isSuccess = Convert.toBool(success);
            if (!isSuccess) {
                Object errors = ResponseStatusObj.get("Errors");
                JSONArray errorsArr = JSONUtil.parseArray(errors);
                StringBuffer errMsg = new StringBuffer();
                for (int i = 0; i < errorsArr.size(); i++) {
                    errMsg.append(JSONUtil.parseObj(errorsArr.get(i)).get("Message") + ",");
                }
                throw new Exception(errMsg.toString());
            }
            actionResult.success();
        } catch (Exception e) {
            actionResult.failure(e.getMessage());
        }
        long end = System.currentTimeMillis();
        logger.info("金蝶凭证集成耗费时间:" + (end - start) / 1000.0);
        logger.info("金蝶凭证集成: end");
        return doResult(actionResult.getStatus(), actionResult.getMessage(), requestInfo.getRequestManager());
    }

    public static String isNullString(Object value) {
        if (value != null) {
            return String.valueOf(value);
        }
        return "";
    }

    public static void main(String[] args) {
        String requestName="CW03-对公支付流程";
        System.out.println(requestName.contains("对公支付流程"));
    }


}
