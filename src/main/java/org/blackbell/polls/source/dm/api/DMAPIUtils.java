package org.blackbell.polls.source.dm.api;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMAPIUtils {
    public static String getDMMeetingsRequestUrl(Town city, InstitutionType institution, String season) {
        if (InstitutionType.KOMISIA.equals(institution)) {
            return Constants.DM_COMMISION_MEETINGS_REQUEST_URL
                    .replaceAll("\\{city\\}", city.getRef())
                    .replaceAll("\\{season\\}", season);
        }
        return Constants.DM_MEETINGS_REQUEST_URL
                .replaceAll("\\{city\\}", city.getRef())
                .replaceAll("\\{institution\\}", institution.toDMValue())
                .replaceAll("\\{season\\}", season);
    }

    public static String getDMCitiesRequestUrl() {
        return Constants.DM_CITIES_REQUEST_URL;
    }

    public static String getDMCityRequestUrl(String dmCityId) {
        return Constants.DM_CITY_REQUEST_URL;
    }

    public static String getDMSeasonsRequestUrl(Town city) {
        return Constants.DM_SEASONS_REQUEST_URL.replaceAll("\\{city\\}", city.getRef());
    }

    public static String getDMMeetingDetailRequestUrl(String dmMettingId) {
        return Constants.DM_MEETING_REQUEST_URL.replaceAll("\\{dm_meeting_id\\}", dmMettingId);
    }

    public static String getDMPollDetailRequestUrl(String dmPollId, String pollRoute) {
        return Constants.DM_POLL_REQUEST_URL
                .replaceAll("\\{dm_agenda_item_id\\}", dmPollId)
                .replaceAll("\\{dm_poll_route\\}", pollRoute);
    }
}
