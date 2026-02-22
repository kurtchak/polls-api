package org.blackbell.polls.source.dm.api;

import org.blackbell.polls.config.DmApiProperties;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.springframework.stereotype.Component;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
@Component
public class DMAPIUtils {

    private final DmApiProperties dmApiProperties;

    public DMAPIUtils(DmApiProperties dmApiProperties) {
        this.dmApiProperties = dmApiProperties;
    }

    public String getDMMeetingsRequestUrl(Town city, InstitutionType institution, String season) {
        if (InstitutionType.KOMISIA.equals(institution)) {
            return dmApiProperties.getCommissionMeetingsUrl(city.getRef(), season);
        }
        return dmApiProperties.getMeetingsUrl(city.getRef(), institution.toDMValue(), season);
    }

    public String getDMCitiesRequestUrl() {
        return dmApiProperties.getCitiesUrl();
    }

    public String getDMSeasonsRequestUrl(Town city) {
        return dmApiProperties.getSeasonsUrl(city.getRef());
    }

    public String getDMMeetingDetailRequestUrl(String dmMeetingId) {
        return dmApiProperties.getMeetingDetailUrl(dmMeetingId);
    }

    public String getDMPollDetailRequestUrl(String dmPollId, String pollRoute) {
        return dmApiProperties.getPollDetailUrl(dmPollId, pollRoute);
    }
}
