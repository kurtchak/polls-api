package org.blackbell.polls.meetings.dm.api;

import org.blackbell.polls.common.Constants;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMAPIUtils {
    public static String getDMMeetingsRequestUrl(String city, String institution) {
        return Constants.DM_MEETINGS_REQUEST_URL.replace("{city}", city).replace("{institution}", institution);
    }
}
