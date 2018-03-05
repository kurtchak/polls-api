package org.blackbell.polls.meetings.source.dm.api;

import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingsResponse;
import org.blackbell.polls.meetings.source.dm.api.response.DMTownResponse;
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
            throw new Exception("No town loaded");
        }
        return meetingsResponse;
    }

    public static DMTownResponse checkoutTownData(String dmCityId) throws Exception {
        String url = DMAPIUtils.getDMCityRequestUrl(dmCityId);
        System.out.println(">> checkoutTownData >> " + url);
        DMTownResponse townResponse = new RestTemplate().getForObject(url, DMTownResponse.class);
        if (townResponse == null) {
            throw new Exception("No town loaded");
        }
        return townResponse;
    }

    public static DMMeetingResponse checkoutMeetingData(String dmMeetingId) throws Exception {
        String url = DMAPIUtils.getDMMeetingDetailRequestUrl(dmMeetingId);
        System.out.println(">> checkoutMeetingData >> " + url);
        DMMeetingResponse meetingReponse = new RestTemplate().getForObject(url, DMMeetingResponse.class);
        if (meetingReponse == null) {
            throw new Exception("No town loaded");
        }
        return meetingReponse;
    }
}
