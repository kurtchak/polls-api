package org.blackbell.polls.common;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class PollsUtils {
    private static String cutDateStringToTFormat(String dateString) {
        return dateString.substring(0,19);
    }

    public static Date parseDMDate(String dmDate) throws ParseException {
        return Constants.FULLDATE_WITH_T_FORMAT.parse(cutDateStringToTFormat(dmDate));
    }

    public static Date parseSimpleDate(String dmDate) throws ParseException {
        return Constants.DATE_FORMAT.parse(dmDate);
    }

    public static String getSimpleName(String name) {
        String result = name;
        Pattern titleRE = Pattern.compile("(\\w+\\.)|MPH|MBA|DBA|Mgr|PhDr");
        Matcher m = titleRE.matcher(name);
        while (m.find()) {
            result = result.replace(m.group(), "");
        }
        return result.replaceAll(",", "")
                .replaceAll("  ", " ")
                .trim();
    }

    public static String startWithFirstname(String fullname) {
        String[] name = fullname.split("\\s", 2);
        return String.join(" ", Arrays.asList(name[1], name[0]));
    }

    public static String getTitles(String name) {
        String result = "";
        Pattern titleRE = Pattern.compile("(\\w+\\.)|MPH|MBA|DBA|Mgr|PhDr");
        Matcher m = titleRE.matcher(name);
        while (m.find()) {
            String title = !m.group().endsWith(".") ? m.group() + "." : m.group();
            result += title + ", ";
        }
        return !result.isEmpty() ? result.substring(0, result.length() - 2) : null;
    }

    public static String generateUniqueKeyReference() {
        return "" + System.nanoTime();
    }

    public static List<String> splitCleanAndTrim(String value) {
        return Arrays.asList(value.split("\\s*,\\s*|\\s+a\\s+")).stream().map(item -> cleanAndTrim(item)).collect(Collectors.toList());
    }

    public static String generateClubName(List<String> partyList) {
        return String.join("/", partyList);
    }

    public static String cleanAndTrim(String item) {
        return item.replaceAll("\\s*-\\s*", "-").replaceAll("\\s*–\\s*", "-");
    }
}
