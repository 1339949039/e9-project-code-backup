package com.api.sso.mailboxsso.web;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.api.sso.util.SsoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.util.Map;

/**
 * @author Li Yu Feng
 * @date 2023-02-09 10:31
 */
@Path("/mail")
public class MailboxSsoController {
    // 获取自定义的 logger, 其中 debug为配置文件中 log4j.logger.debug中的debug
    Logger logger = LoggerFactory.getLogger("lyflog");

    @GET
    @Path("/mailboxSso")
    public void AutoLogin(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        logger.info("邮箱单点登录: start");
        User user = HrmUserVarify.getUser(request, response);
        try {
            if (user==null||StrUtil.hasEmpty(user.getEmail())){
                throw new Exception("邮箱不能为空");
            }
            String ssourl = SsoUtil.getMailboxSsoUrl(logger,user.getEmail());
            response.sendRedirect(ssourl);
        } catch (Exception e) {
            logger.error("邮箱单点异常:", e);
        }
        logger.info("邮箱单点登录: end");
    }
}
