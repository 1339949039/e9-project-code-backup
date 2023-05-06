package com.api.sso.test;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Li Yu Feng
 * @date 2023-02-27 15:25
 */
public class Test02 {
    public void getUrl(String usserName) throws Exception {
            String dbId = "1590564251473317888";//"1590564251473317888"; //数据中心ID
        //X-KDApi-AppID=242798_wd3NX+DG3uhZX+1uWY4L4b8uUt4c0KLp
        //X-KDApi-AppSec=c2d915d6c438405a953c847b05a11f4b
            String appId = "242798_wd3NX+DG3uhZX+1uWY4L4b8uUt4c0KLp";//"241657_Rdco3ZDITkBYwXWG3f0pUcUIQIxU6trE"; //第三方系统应用Id
            String appSecret = "c2d915d6c438405a953c847b05a11f4b";//"b12c82a2b552417893b1f532124ec5ea"; //第三方系统应用秘钥
            String kingdeeUrl = "https://poissonsoft.ik3cloud.com/k3cloud/html5/Index.aspx?ud=";//"https://poissonsoft.ik3cloud.com/k3cloud/html5/Index.aspx?ud="; //金蝶云域名地址

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
        System.out.println("url = " + url);
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

    public static void main(String[] args) throws Exception {
        //new Test02().getUrl("赵欢元");










    }
}
