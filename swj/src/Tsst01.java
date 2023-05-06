import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import weaver.common.StringUtil;
import weaver.file.ImageFileManager;
import weaver.general.GCONST;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Li Yu Feng
 * @date 2023-03-07 16:06
 */
public class Tsst01 {
    private  String getValue(Map<String, Object> mainTableMap,String key){
        if (key!=null){
            return mainTableMap.get(key)==null?"":String.valueOf(mainTableMap.get(key));
        }
        return "";
    }
    private String getTheTypeOfOperation(String logtype){
        String operationName="";
        switch (logtype){
            /**
             * 0：未操作
             * 1：转发
             * 2：已操作
             * 4：归档
             * 5：超时
             * 8：抄送(不需提交)
             * 9：抄送(需提交)
             * a: 意见征询
             * b: 回复
             * h: 转办
             * j: 转办提交
             * 11:传阅
             * 6:自动审批（审批中）
             */
            case "0":
                operationName="未操作";
                break;
            case "1":
                operationName="转发";
                break;
            case "2":
                operationName="已操作";
                break;
            case "4":
                operationName="归档";
                break;
            case "5":
                operationName="超时";
                break;
            case "8":
                operationName="抄送(不需提交)";
                break;
            case "9":
                operationName="抄送(需提交)";
                break;
            case "a":
                operationName="意见征询";
                break;
            case "b":
                operationName="回复";
                break;
            case "h":
                operationName="转办";
                break;
            case "j":
                operationName="转办提交";
                break;
            case "11":
                operationName="传阅";
                break;
            case "6":
                operationName="自动审批（审批中）";
                break;
            default:
                break;
        }
        return operationName;
    }
    public static void main(String[] args) {
        long timestamp=System.currentTimeMillis();
        System.out.println("timestamp = " + timestamp);
        String key= "archiveskey123";
        System.out.println("key = " + key);
        String sign=SecureUtil.sha1(timestamp+key);
        System.out.println("sign = " + sign);
        System.out.println("sign = " + sign.equals("abc04cead1726dcdc22f6261e63bf68c6d03aca2"));



    }
    public void sendPost(){
        //随机生成4位数字
        String x_nonce=String.valueOf((new Random().nextInt(9000) + 1000));
        System.out.println("x_nonce = " + x_nonce);
        //生成时间戳
        String x_timestamp= String.valueOf(Instant.now().getEpochSecond());
        System.out.println("x_timestamp = " + x_timestamp);
        List<File> files=new ArrayList<>();
        files.add(new File("C:\\Users\\13399\\Desktop\\测试文档.docx"));
        files.add(new File("C:\\Users\\13399\\Desktop\\测试文档.txt"));
        File[] fileArray = files.toArray(new File[0]);
        String result = HttpRequest.post("http://3a8b0084.r8.cpolar.top/tyarchive/preGd/savePreData")
                .header("x-apiKey","3d6c7fa54d694e419ae71d7e80940c58")
                .header("x-sign","1dcab2b01ccef43bed708c7bd1df4c9e")
                .header("x-nonce",x_nonce)
                .header("x-timestamp",x_timestamp)
                .form("data","{\"ywid\":\"210210\",\"process\":[{\"处理类型\":\"转发\",\"意见类型\":\"\",\"处理结果\":\"\",\"处理时间\":\"2023-03-09 09:56:15\",\"处理部门\":\"\",\"处理者\":\"系统管理员\"}],\"sxlx\":\"公文归档\",\"version\":\"v1\",\"result\":{\"办理结果\":\"\",\"有效期\":\"\"},\"files\":[{\"filetype\":\"\",\"filesize\":\"14224\",\"filepath\":\"/opt/weaver/ecology//filesystem/hnfileTemp/测试档案系统.docx\",\"disname\":\"测试档案系统.docx\",\"md5\":\"\"},{\"filetype\":\"\",\"filesize\":\"15\",\"filepath\":\"/opt/weaver/ecology//filesystem/hnfileTemp/测试文档.txt\",\"disname\":\"测试文档.txt\",\"md5\":\"\"}],\"basic\":{\"稿本\":\"\",\"计算机文件大小\":\"\",\"紧急程度\":\"加急\",\"抄送机关\":\"\",\"原稿格式\":\"\",\"收发文\":\"formtable_main_178\",\"发文机关或签发人签名\":\"深圳市水务局\",\"成文日期\":\"\",\"计算机文件形成时间\":\"2023-03-09 10:00:36\",\"印发日期\":\"\",\"份号\":\"\",\"计算机文件名\":\"\",\"件内顺序号\":\"\",\"签发人\":\"\",\"公开选项\":\"主动公开\",\"收取方式\":\"在线生成\",\"文档创建程序[软件环境]\":\"\",\"文件编号[文号]\":\"\",\"主送机关\":\"的撒哈拉\",\"题名\":\"测试档案系统\",\"文种\":\"\",\"密级\":\"\",\"印发机关\":\"\",\"公文标识\":\"210210\",\"计算机文件格式信息\":\"\",\"发文机关标志\":\"\"}}")
                .form("files",fileArray)
                .execute().body();
        System.out.println("result = " + result);
    }
    private void getValue(String ...docids){
        for (String docid : docids) {
            System.out.println("docid = " + docid);
        }

    }
}
