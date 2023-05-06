package com.api.autologin.web;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.api.autologin.util.LoginUtil;
import com.weaver.general.Util;
import weaver.file.Prop;
import weaver.general.BaseBean;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 
 * @date 2022-09-05 17:40
 */
@Path("/autologin")
public class AutoLoginWeb {

    private static final String CONFIG = "AutoLogin";

    //获取Token地址
    String getTokenUrl= Util.null2String(Prop.getPropValue(CONFIG, "getTokenUrl"));

    //效验Token地址
    String checkTokenUrl= Util.null2String(Prop.getPropValue(CONFIG, "checkTokenUrl"));

    //统建应用标识
    String AutoAppid= Util.null2String(Prop.getPropValue(CONFIG, "AutoAppid"));


    /**
     * 自动登录接口 http://192.168.137.129:8080/api/autologin/AutoLogin?loginid=fchzh
      * @param request
     * @param response
     */
    @GET
    @Path("/AutoLogin")
    public void AutoLogin(@Context HttpServletRequest request, @Context HttpServletResponse response){
        BaseBean log = new BaseBean();
        String loginid = request.getParameter("loginid");
        log.writeLog("免登录接口: start   loginid:"+loginid);
        Object data=null;
        try {
            //loginid为空跳转首页
            if(StrUtil.hasEmpty(loginid)){
                User user = HrmUserVarify.getUser(request, response);
                if (user!=null){
                    loginid=user.getLoginid();
                } else {
                    response.sendRedirect("/wui/index.html");
                    return;
                }
            }
            log.writeLog("AutoAppid:"+AutoAppid);
            Map<String,Object> formMap=new HashMap<>();
            formMap.put("appid",AutoAppid);
            formMap.put("loginid",loginid);
            log.writeLog("getTokenUrl:"+getTokenUrl);
            //获取Token
            String token = LoginUtil.sendGetRequest(formMap, getTokenUrl);
            if (!token.matches("^[a-z0-9A-Z]+$")){
                throw new Exception(token);
            }
            log.writeLog("checkTokenUrl:"+checkTokenUrl);

            Map<String,Object> check=new HashMap<>();
            check.put("token",token);
            //效验Token
            String checkToken = LoginUtil.sendGetRequest(check, checkTokenUrl);
            log.writeLog("checkToken:"+checkToken);
            Boolean checkTokenBool = Convert.toBool(checkToken);
            if (!checkTokenBool){
                throw new Exception("token已失效");
            }
            //效验通过，跳转localhost:8080/spa/workflow/static4form/index.html#/main/workflow	/req?iscreate=1&workflowid=10
            String sendUrl="/spa/workflow/static4form/index.html?ssoToken="+token+"#/main/workflow/req?iscreate=1&workflowid=10";
            log.writeLog("免登录接口: end sendUrl:"+sendUrl);
            response.sendRedirect(sendUrl);
        } catch (Exception e) {
            log.errorLog("免登录接口异常: ",e);

        }
    }

}
