package org.blackbell.polls.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by Ján Korčák on 1.3.2017.
 * email: korcak@esten.sk
 */
public class Constants {
    public static final String TIMESTAMP_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final DateFormat TIMESTAMP_FORMAT;

    static {
        TIMESTAMP_FORMAT = new SimpleDateFormat(TIMESTAMP_FORMAT_PATTERN);
    }

}
