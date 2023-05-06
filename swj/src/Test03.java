import weaver.common.StringUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author Li Yu Feng
 * @date 2023-03-10 17:07
 */
public class Test03 {
    public static void main(String[] args) {
        String cwrq="2022-08-26";
        if (StringUtil.isNotNull(cwrq)) {
            cwrq = LocalDate.parse(cwrq).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        System.out.println("cwrq = " + cwrq);


    }
}
