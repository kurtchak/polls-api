package org.blackbell.polls.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Ján Korčák on 1.3.2017.
 * email: korcak@esten.sk
 */
public class Constants {
    public static final String FULLDATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateFormat FULLDATE_FORMAT;

    public static final String FULLDATE_WITH_T_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateFormat FULLDATE_WITH_T_FORMAT;

    public static final String DM_MEETINGS_REQUEST_URL = "https://mesto-{city}.digitalnemesto.sk/DmApi/GetDZZasadnutie/{institution}/mesto-{city}";

    static {
        FULLDATE_FORMAT = new SimpleDateFormat(FULLDATE_FORMAT_PATTERN);
        FULLDATE_WITH_T_FORMAT = new SimpleDateFormat(FULLDATE_WITH_T_FORMAT_PATTERN);
    }

}
