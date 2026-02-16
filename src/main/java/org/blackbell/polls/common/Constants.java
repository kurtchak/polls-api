package org.blackbell.polls.common;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * Created by Ján Korčák on 1.3.2017.
 * email: korcak@esten.sk
 */
public class Constants {
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    public static final DateFormat DATE_FORMAT;

    public static final String FULLDATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateFormat FULLDATE_FORMAT;

    public static final String FULLDATE_WITH_T_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateFormat FULLDATE_WITH_T_FORMAT;

    public static final String[] TITLES = new String[]{"MBA", "MPH", "Phd.", "PhDr.", "PhD.", "PaedDr.", "RNDr.", "Ing.", "Mgr.", "JUDr.", "MUDr.", "doc.", "Csc."};
    public static final String TITLE_RE = "(\\w+\\.)|MPH|MBA|DBA|Mgr|PhDr";
    public static final Pattern TITLE_PATTERN = Pattern.compile(Constants.TITLE_RE);

    public static final Marker MarkerSync = MarkerFactory.getMarker("SYNC");

    static {
        DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        FULLDATE_FORMAT = new SimpleDateFormat(FULLDATE_FORMAT_PATTERN);
        FULLDATE_WITH_T_FORMAT = new SimpleDateFormat(FULLDATE_WITH_T_FORMAT_PATTERN);
    }

    public static final String ZASTUPITELSTVO = "zastupitelstvo";
    public static final String RADA = "rada";
    public static final String KOMISIA = "komisia";

    public static final String DM_ZASTUPITELSTVO = "mz";
    public static final String DM_RADA = "mr";
    public static final String DM_KOMISIA = "k";

}
