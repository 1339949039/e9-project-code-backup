import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author
 * @date 2022-09-05 17:49
 */
public class Test {
    public static void main(String[] args) {
        String localUrl = "http://192.168.137.129:8080/rest/ofs/ReceiveRequestInfoByMap";//
        String localUrl2 = "http://192.168.137.129:8080/rest/ofs/ReceiveRequestInfoByJson";//

        //System.out.println("json:"+json);
        String timestamp=System.currentTimeMillis()+"";
        String pcUrl="/spa/workflow/static4form/index.html?_rdm="+timestamp+"#/main/workflow/req?requestid=6007&preloadkey="+timestamp+"&timestamp="+timestamp;
        String mobileUrl="/spa/workflow/static4mobileform/index.html?_random="+timestamp+"#/req?requestid=6007&f_weaver_belongto_userid=1&f_weaver_belongto_usertype=&timestamp="+timestamp;

        String uuid = IdUtil.simpleUUID();
        Map<String,Object> data=new HashMap<>();
        data.put("syscode","lyfProcess");
        data.put("flowid",uuid);//唯一id
        data.put("requestname","李玉锋测试标题");//流程标题
        data.put("workflowname","测试流程");//流程名称
        data.put("nodename","测试节点");//流程节点名称
        data.put("pcurl",pcUrl);//pcurl
        data.put("appurl",mobileUrl);//移动端url
        data.put("creator","lchh");//创建人
        data.put("createdatetime",DateUtil.now());//创建日期时间
        data.put("receiver","lchh");//接收人
        data.put("receivedatetime",DateUtil.now());//接收日期时间
        data.put("isremark","0");// 0 待办 2：已办 4：办结
        data.put("viewtype","0");//流程查看状态 0：未读 1：已读;
        data.put("receivets",timestamp);//当前时间戳
        String toJsonStrData = JSONUtil.toJsonStr(data);
        System.out.println(toJsonStrData);
        String result2 = HttpRequest.post(localUrl)
                .form(data)
                .execute().body();
        System.out.println("待办结果：" + result2);
    }
}
