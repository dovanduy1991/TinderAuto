package name.alexy.test.tinderauto.phoneservice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alexeykrichun on 11/10/2017.
 */

public class FacebookSmsParser extends SmsParser {
    @Override
    protected String parseCode(String text) {
        if (text.toLowerCase().contains("facebook")) {
            Pattern pattern = Pattern.compile("\\b(\\d{5})\\b");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                System.out.println("Code found " + matcher.group());
                return matcher.group();
            }
        }
        return null;
    }
}
