package org.blackbell.polls.common;

import org.blackbell.polls.meetings.source.dm.dto.MeetingDTO;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class PollDateUtils {
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
}
