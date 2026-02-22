package org.blackbell.polls.source.dm.api;

import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.source.dm.api.response.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
@Component
public class DMServiceClient {
    private static final Logger log = LoggerFactory.getLogger(DMServiceClient.class);

    private final DMAPIUtils dmApiUtils;

    public DMServiceClient(DMAPIUtils dmApiUtils) {
        this.dmApiUtils = dmApiUtils;
    }

    public DMMeetingsResponse checkoutMeetingsData(Town city, InstitutionType institution, String season) throws Exception {
        String url = dmApiUtils.getDMMeetingsRequestUrl(city, institution, season);
        log.debug("checkoutMeetingsData >> {}", url);
        DMMeetingsResponse meetingsResponse = new RestTemplate().getForObject(url, DMMeetingsResponse.class);
        if (meetingsResponse == null) {
            throw new Exception("No meetings loaded");
        }
        return meetingsResponse;
    }

    public DMMeetingResponse checkoutMeetingData(String dmMeetingId) throws Exception {
        String url = dmApiUtils.getDMMeetingDetailRequestUrl(dmMeetingId);
        log.debug("checkoutMeetingData >> {}", url);
        DMMeetingResponse meetingReponse = new RestTemplate().getForObject(url, DMMeetingResponse.class);
        if (meetingReponse == null) {
            throw new Exception("No meeting loaded");
        }
        return meetingReponse;
    }

    public DMPollDetailResponse checkoutPollData(String extAgendaItemId, String pollName) throws Exception {
        String url = dmApiUtils.getDMPollDetailRequestUrl(extAgendaItemId, pollName);
        log.debug("checkoutPollData >> {}", url);
        DMPollDetailResponse pollReponse = new RestTemplate().getForObject(url, DMPollDetailResponse.class);
        if (pollReponse == null) {
            throw new Exception("No poll loaded");
        }
        return pollReponse;
    }

    public DMTownsResponse checkoutTownsData() throws Exception {
        String url = dmApiUtils.getDMCitiesRequestUrl();
        log.debug("checkoutTownsData >> {}", url);
        DMTownsResponse townsResponse = new RestTemplate().getForObject(url, DMTownsResponse.class);
        if (townsResponse == null) {
            throw new Exception("No town loaded");
        }
        return townsResponse;
    }

    public DMSeasonsResponse checkoutSeasonsData(Town town) throws Exception {
        String url = dmApiUtils.getDMSeasonsRequestUrl(town);
        log.debug("checkoutSeasonsData >> {}", url);
        DMSeasonsResponse seasonsResponse = new RestTemplate().getForObject(url, DMSeasonsResponse.class);
        if (seasonsResponse == null) {
            throw new Exception("No town loaded");
        }
        return seasonsResponse;
    }
}
