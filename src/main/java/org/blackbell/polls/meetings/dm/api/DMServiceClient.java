package org.blackbell.polls.meetings.dm.api;

import org.blackbell.polls.meetings.dm.DMMeetingsResponse;
import org.blackbell.polls.meetings.model.Institution;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class DMServiceClient {
    public static DMMeetingsResponse checkoutMeetingsData(String city, Institution institution) throws Exception {
        String url = DMAPIUtils.getDMMeetingsRequestUrl(city, institution);
        System.out.println("URL: " + url);
        DMMeetingsResponse meetingsResponse = new RestTemplate().getForObject(url, DMMeetingsResponse.class);
        System.out.println("meetingResponse: " + meetingsResponse);
        if (meetingsResponse == null) {
            throw new Exception("No town loaded");
        }
        return meetingsResponse;
    }
}
