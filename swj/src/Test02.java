import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Li Yu Feng
 * @date 2023-03-10 16:48
 */
public class Test02 {
    public static void main(String[] args) {
        /*String url="http://10.224.153.72:8089/api/archives/updateArchivesInfo";

        Map<String,Object> map=new HashMap<>();
        map.put("dataName","测试.txt");
        map.put("result","1");
        map.put("stepcode","1");
        map.put("docNo","cesa2134222");
        long timestamp=System.currentTimeMillis();
        System.out.println("timestamp = " + timestamp);
        String key= "archiveskey123";
        System.out.println("key = " + key);
        String sign= SecureUtil.sha1(timestamp+key);
        System.out.println("sign = " + sign);
        String result2 = HttpRequest.post(url)
                .header("timestamp",String.valueOf(timestamp))//头信息，多个头信息多次调用此方法即可
                .header("sign",sign)
                .form(map)//表单内容
                .execute().body();
        System.out.println("result2 = " + result2);*/
        //Document doc = Jsoup.parse("<span id=\"wea_rich_text_default_font\" style=\"font-size:16px;\">同意。</span>");




    }

    public String getText(String htmlText){
        if (StrUtil.hasEmpty(htmlText)){
            return "";
        }
        Document doc = Jsoup.parse(htmlText);
        Elements elements = doc.body().select("*");
        for (int i = 0; i < elements.size(); i++) {
            Element el=elements.get(i);
            String text = el.ownText();
            if (!StrUtil.hasEmpty(text)) {
                return text;
            }
        }
        return "";
    }
}
