package com.api.sso.test;

import cn.hutool.crypto.SecureUtil;

import java.time.Instant;
import java.util.Random;

/**
 * @author Li Yu Feng
 * @date 2023-02-27 10:55
 */
public class Test01 {


    public static void main(String[] args) {

        String x_apiKey="3d6c7fa54d694e419ae71d7e80940c58";
        String secretKey="1dcab2b01ccef43bed708c7bd1df4c9e";
        //随机生成4位数字
        String x_nonce=String.valueOf((new Random().nextInt(9000) + 1000));
        System.out.println("x_nonce = " + x_nonce);
        //生成时间戳
        String x_timestamp= String.valueOf(Instant.now().getEpochSecond());
        System.out.println("x_timestamp = " + x_timestamp);

        /**
         * 将：apiKey- secretkey- timestamp-nonce拼接后计算出来的MD5
         */
        String splice=x_apiKey+"-"+secretKey+"-"+x_timestamp+"-"+x_nonce;
        String x_sign=SecureUtil.md5(splice);
        System.out.println("生成签名 = " + x_sign);

    }
}
