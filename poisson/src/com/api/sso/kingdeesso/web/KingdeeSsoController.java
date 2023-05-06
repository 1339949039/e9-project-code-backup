package com.api.sso.kingdeesso.web;

import cn.hutool.core.util.StrUtil;
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

/**金蝶云单点登录
 * @author Li Yu Feng
 * @date 2023-02-09 15:29
 */
@Path("/kingdee")
public class KingdeeSsoController {
    // 获取自定义的 logger,
    Logger logger = LoggerFactory.getLogger("lyflog");

    @GET
    @Path("/kingdeeSso")
    public void AutoLogin(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        logger.info("金蝶云单点登录: start");
        User user = HrmUserVarify.getUser(request, response);
        try {
            if (user==null||StrUtil.hasEmpty(user.getLastname())){
                throw new Exception("用户名不能为空");
            }
            String ssourl = SsoUtil.getKingdeeSsourl(logger, user.getLastname());
            response.sendRedirect(ssourl);
        } catch (Exception e) {
            logger.error("金蝶云单点异常:", e);
        }
        logger.info("金蝶云单点登录: end");
    }
}
