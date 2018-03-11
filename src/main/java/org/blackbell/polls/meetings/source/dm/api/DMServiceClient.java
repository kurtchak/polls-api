package org.blackbell.polls.meetings.source.dm.api;

import org.blackbell.polls.meetings.source.dm.api.response.*;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Town;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMServiceClient {
    public static DMMeetingsResponse checkoutMeetingsData(Town city, Institution institution, String season) throws Exception {
        String url = DMAPIUtils.getDMMeetingsRequestUrl(city, institution, season);
        System.out.println(">> checkoutMeetingsData >> " + url);
        DMMeetingsResponse meetingsResponse = new RestTemplate().getForObject(url, DMMeetingsResponse.class);
        if (meetingsResponse == null) {
            throw new Exception("No meetings loaded");
        }
        return meetingsResponse;
    }
/*
    public static DMTownsResponse checkoutTownData(String dmCityId) throws Exception {
        String url = DMAPIUtils.getDMCityRequestUrl(dmCityId);
        System.out.println(">> checkoutTownData >> " + url);
        DMTownsResponse townResponse = new RestTemplate().getForObject(url, DMTownsResponse.class);
        if (townResponse == null) {
            throw new Exception("No town loaded");
        }
        return townResponse;
    }
*/
    public static DMMeetingResponse checkoutMeetingData(String dmMeetingId) throws Exception {
        String url = DMAPIUtils.getDMMeetingDetailRequestUrl(dmMeetingId);
        System.out.println(">> checkoutMeetingData >> " + url);
        DMMeetingResponse meetingReponse = new RestTemplate().getForObject(url, DMMeetingResponse.class);
        if (meetingReponse == null) {
            throw new Exception("No meeting loaded");
        }
        return meetingReponse;
    }

    public static DMPollDetailResponse checkoutPollData(String extAgendaItemId, String pollName) throws Exception {
        String url = DMAPIUtils.getDMPollDetailRequestUrl(extAgendaItemId, pollName);
        System.out.println(">> checkoutPollData >> " + url);
        DMPollDetailResponse pollReponse = new RestTemplate().getForObject(url, DMPollDetailResponse.class);
        if (pollReponse == null) {
            throw new Exception("No poll loaded");
        }
        return pollReponse;
    }

    public static DMTownsResponse checkoutTownsData() throws Exception {
        String url = DMAPIUtils.getDMCitiesRequestUrl();
        System.out.println(">> checkoutTownsData >> " + url);
        DMTownsResponse townsResponse = new RestTemplate().getForObject(url, DMTownsResponse.class);
        if (townsResponse == null) {
            throw new Exception("No town loaded");
        }
        return townsResponse;
    }

    public static DMSeasonsResponse checkoutSeasonsData(Town town) throws Exception {
        String url = DMAPIUtils.getDMSeasonsRequestUrl(town);
        System.out.println(">> checkoutSeasonsData >> " + url);
        DMSeasonsResponse seasonsResponse = new RestTemplate().getForObject(url, DMSeasonsResponse.class);
        if (seasonsResponse == null) {
            throw new Exception("No town loaded");
        }
        return seasonsResponse;
    }
}
