package org.blackbell.polls.meetings.source.dm.api;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Town;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMAPIUtils {
    public static String getDMMeetingsRequestUrl(Town city, Institution institution, String season) {
        if (Institution.KOMISIA.equals(institution)) {
            return Constants.DM_COMMISION_MEETINGS_REQUEST_URL
                    .replaceAll("\\{city\\}", city.getName())
                    .replaceAll("\\{season\\}", season);
        }
        return Constants.DM_MEETINGS_REQUEST_URL
                .replaceAll("\\{city\\}", city.getName())
                .replaceAll("\\{institution\\}", institution.getDMValue())
                .replaceAll("\\{season\\}", season);
    }

    public static String getDMCitiesRequestUrl() {
        return Constants.DM_CITIES_REQUEST_URL;
    }

    public static String getDMCityRequestUrl(String dmCityId) {
        return Constants.DM_CITY_REQUEST_URL;
    }

    public static String getDMSeasonsRequestUrl(Town city) {
        return Constants.DM_SEASONS_REQUEST_URL.replaceAll("\\{city\\}", city.getName());
    }

    public static String getDMMeetingDetailRequestUrl(String dmMettingId) {
        return Constants.DM_MEETING_REQUEST_URL.replaceAll("\\{dm_meeting_id\\}", dmMettingId);
    }

    public static String getDMPollDetailRequestUrl(String dmPollId, String pollName) {
        return Constants.DM_POLL_REQUEST_URL.replaceAll("\\{dm_poll_id\\}", dmPollId);
    }
}
