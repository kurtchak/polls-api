package org.blackbell.polls.common;

import org.blackbell.polls.meetings.source.dm.dto.MeetingDTO;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class PollsUtils {
    private static String cutDateStringToTFormat(String dateString) {
        return dateString.substring(0,19);
    }

    public static Date parseMeetingDate(MeetingDTO meetingDTO) throws ParseException {
        return Constants.FULLDATE_WITH_T_FORMAT.parse(cutDateStringToTFormat(meetingDTO.getDate()));
    }

    public static Date parseDMDate(String dmDate) throws ParseException {
        return Constants.FULLDATE_WITH_T_FORMAT.parse(cutDateStringToTFormat(dmDate));
    }

    public static Date parseSimpleDate(String dmDate) throws ParseException {
        return Constants.DATE_FORMAT.parse(dmDate);
    }

    public static String getSimpleName(String name) {
        String result = name;
        Pattern titleRE = Pattern.compile("(\\w+\\.|MPH|MBA|DBA)");
        Matcher m = titleRE.matcher(name);
        while (m.find()) {
            result = result.replaceAll(m.group(), "");
        }
        return result.replaceAll(",", "")
                .replaceAll("  ", " ")
                .trim();
    }

    public static String getTitles(String name) {
        String result = "";
        Pattern titleRE = Pattern.compile("(\\w+\\.|MPH|MBA|DBA)");
        Matcher m = titleRE.matcher(name);
        while (m.find()) {
            result += m.group() + ", ";
        }
        return !result.isEmpty() ? result.substring(0, result.length() - 2) : "";
    }

    public static String generateUniqueKeyReference() {
        return "" + System.nanoTime();
    }
}
