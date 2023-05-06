package com.api.sso.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.slf4j.Logger;
import weaver.conn.RecordSet;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.IOException;
import java.util.*;
import java.net.URLEncoder;

/**
 * @author Li Yu Feng
 * @date 2023-02-09 11:02
 */
public class SsoUtil {
    /**
     * 获取动态参数
     * @param logger
     * @param key
     * @return
     * @throws Exception
     */
    public static Map<String, String> getDynamicParameters(Logger logger, String key) throws Exception {
        logger.info("key：" + key);
        Map<String, String> map = new HashMap<>();
        RecordSet rs = new RecordSet();
        String sql = "SELECT value1,value2,value3,value4 FROM uf_Parameter WHERE cskey=?";
        rs.executeQuery(sql, key);
        if (rs.next()) {
            map.put("value1", rs.getString(1));
            map.put("value2", rs.getString(2));
            map.put("value3", rs.getString(3));
            map.put("value4", rs.getString(4));

        }
        logger.info("value：" + JSONUtil.toJsonStr(map));
        return map;
    }

    /**
     * 单点登录金蝶
     * @param logger
     * @param usserName 用户名
     * @return
     * @throws Exception
     */
    public static String getKingdeeSsourl(Logger logger, String usserName) throws Exception {
        Map<String, String> ssoParameter = getDynamicParameters(logger, "kingdeeCloudSsoParameter");

        String dbId = ssoParameter.get("value1");//"1590564251473317888"; //数据中心ID
        if (StrUtil.hasEmpty(dbId)) {
            throw new Exception("数据中心ID不能为空");
        }
        String appId = ssoParameter.get("value2");//"241657_Rdco3ZDITkBYwXWG3f0pUcUIQIxU6trE"; //第三方系统应用Id
        if (StrUtil.hasEmpty(appId)) {
            throw new Exception("第三方系统应用Id不能为空");
        }
        String appSecret = ssoParameter.get("value3");//"b12c82a2b552417893b1f532124ec5ea"; //第三方系统应用秘钥
        if (StrUtil.hasEmpty(appSecret)) {
            throw new Exception("第三方系统应用秘钥不能为空");
        }
        String kingdeeUrl = ssoParameter.get("value4");//"https://poissonsoft.ik3cloud.com/k3cloud/html5/Index.aspx?ud="; //金蝶云域名地址
        if (StrUtil.hasEmpty(kingdeeUrl)) {
            throw new Exception("金蝶云域名地址url不能为空");
        }
        long currentTime = System.currentTimeMillis() / 1000; // 当前时间（秒）
        String timestamp = Long.toString(currentTime);
        String[] strArray = {dbId, usserName, appId, appSecret, timestamp};
        //签名字符串数组需要排序，后生成签名
        Arrays.sort(strArray);
        String combStr = null;
        for (int i = 0; i < strArray.length; i++) {
            if (combStr == null || combStr == "") {
                combStr = strArray[i];
            } else {
                combStr = combStr + strArray[i];
            }
        }
        byte[] strByte = combStr.getBytes("UTF-8");
        byte[] strSign = DigestUtils.sha(strByte);
        String sign = bytesToHexString(strSign);
        String urlPara = String.format("{dbid:'%s',username:'%s',appid:'%s',signeddata:'%s',timestamp:'%s',lcid:'%s',origintype:'simpas'}", dbId, usserName, appId, sign, timestamp, "2052");
        String url = kingdeeUrl + URLEncoder.encode(urlPara, "utf-8");
        logger.info("url:" + url);
        return url;
    }

    /**
     * 单点登录邮箱
     * @param logger
     * @param mail
     * @return
     * @throws Exception
     */
    public static String getMailboxSsoUrl(Logger logger, String mail) throws Exception {
        Map<String, String> ssoParameter = getDynamicParameters(logger, "MailboxSsoParameter");
        String app_id = ssoParameter.get("value1");//public@poissonsoft.com
        String secret = ssoParameter.get("value2");//Public123
        String url = ssoParameter.get("value3");//http://api-c5.icoremail.net/apiws/services/API2
        String mailUrl = ssoParameter.get("value4");//http://api-c5.icoremail.net/coremail/main.jsp?sid=
        if (StrUtil.hasEmpty(app_id)) {
            throw new Exception("app_id不能为空");
        }
        if (StrUtil.hasEmpty(secret)) {
            throw new Exception("secret不能为空");
        }
        if (StrUtil.hasEmpty(url)) {
            throw new Exception("url不能为空");
        }
        if (StrUtil.hasEmpty(mail)) {
            throw new Exception("mail不能为空");
        }
        if (StrUtil.hasEmpty(mailUrl)) {
            throw new Exception("mailUrl不能为空");
        }
        //获取token
        StringBuffer tonkenXml = new StringBuffer();
        tonkenXml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:apiw=\"http://coremail.cn/apiws\">");
        tonkenXml.append("<soapenv:Header/>");
        tonkenXml.append("<soapenv:Body>");
        tonkenXml.append("<apiw:requestToken>");
        tonkenXml.append("<app_id>" + app_id + "</app_id>");
        tonkenXml.append("<secret>" + secret + "</secret>");
        tonkenXml.append("</apiw:requestToken>");
        tonkenXml.append("</soapenv:Body>");
        tonkenXml.append("</soapenv:Envelope>");
        logger.info("tonkenXml = " + tonkenXml.toString());

        String resp = SsoUtil.execute(url, tonkenXml.toString());
        logger.info("resp = " + resp);
        Element rootElement = SsoUtil.getRootElement(resp);
        List<Element> elements = SsoUtil.getElements(rootElement, "return");
        String code = "";
        String result = "";
        for (Element element : elements) {
            Element e1 = element.element("code");
            code = e1.getTextTrim();
            Element e2 = element.element("result");
            result = e2.getTextTrim();
        }
        logger.info("code:" + code);
        logger.info("result:" + result);
        if (StrUtil.hasEmpty(code) || !"0".equals(code)) {
            throw new Exception("请求Token结果异常code:" + code + " result:" + result);
        }
        //请求登录
        StringBuffer loginXml = new StringBuffer();
        loginXml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:apiw=\"http://coremail.cn/apiws\">");
        loginXml.append("<soapenv:Header/>");
        loginXml.append("<soapenv:Body>");
        loginXml.append("<apiw:userLogin>");
        loginXml.append("<_token>" + result + "</_token>");
        loginXml.append("<user_at_domain>" + mail + "</user_at_domain>");
        loginXml.append("</apiw:userLogin>");
        loginXml.append("</soapenv:Body>");
        loginXml.append("</soapenv:Envelope>");
        logger.info("loginXml = " + loginXml.toString());
        String respLogin = SsoUtil.execute(url, loginXml.toString());
        logger.info("respLogin = " + respLogin);
        Element rootElementLogin = SsoUtil.getRootElement(respLogin);
        List<Element> elementsLogin = SsoUtil.getElements(rootElementLogin, "return");
        String codeLogin = "";
        String resultLogin = "";
        for (Element element : elementsLogin) {
            Element e1 = element.element("code");
            codeLogin = e1.getTextTrim();
            Element e2 = element.element("result");
            resultLogin = e2.getTextTrim();
        }
        if (StrUtil.hasEmpty(codeLogin) || !"0".equals(codeLogin)) {
            throw new Exception("请求登录结果异常codeLogin:" + codeLogin + " resultLogin:" + resultLogin);
        }
        logger.info("codeLogin:" + codeLogin);
        logger.info("resultLogin:" + resultLogin);
        String ssoUrl = mailUrl + resultLogin;
        logger.info("ssoUrl = " + ssoUrl);
        return ssoUrl;
    }

    public static String bytesToHexString(byte[] src) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    /**
     * 发送webservice请求
     * @param url
     * @param xml
     * @return
     * @throws IOException
     */
    public static String execute(String url, String xml) throws IOException {
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "text/xml; charset=UTF-8");
        HttpEntity body = new StringEntity(xml);
        post.setEntity(body);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse resp = httpClient.execute(post);
        return EntityUtils.toString(resp.getEntity(), "utf-8");
    }

    public static Element getRootElement(String xml) throws DocumentException {
        Document document = DocumentHelper.parseText(xml);
        return document.getRootElement();
    }

    public static List<Element> getElements(Element root, String name) {
        List<Element> elements = new LinkedList<>();
        getElements(root, name, elements);
        return elements;
    }

    private static void getElements(Element el, String name, List<Element> elements) {
        if (name.equals(el.getName())) {
            elements.add(el);
        } else {
            for (Object element : el.elements()) {
                getElements((Element) element, name, elements);
            }
        }
    }

   /* public static void main(String[] args) throws Exception {
       String encode = URLEncoder.encode("赵欢元", "UTF-8");
       logger.info("encode = " + encode);
       logger.info("url = " + getKingdeeSsourl("赵欢元"));

    }*/
}
