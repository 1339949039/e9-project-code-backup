package com.api.autologin.util;

import cn.hutool.http.HttpRequest;

import java.util.Map;

/**
 * @author Li Yu Feng
 * @date 2022-09-06 14:33
 */
public class LoginUtil {

    public static String sendGetRequest(Map<String,Object> formMap,String url)throws Exception{
        String result = HttpRequest.get(url)
                .form(formMap)//表单内容
                .execute().body();
       return result;
    }

}
