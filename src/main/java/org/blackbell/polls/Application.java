package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.meetings.dm.DMImport;
import org.blackbell.polls.meetings.dm.DMMeetingsResponse;
import org.blackbell.polls.meetings.dm.api.DMServiceClient;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Town;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public static Town loadMeetingsData(Town town, Institution institution) throws Exception {

        // Request Meetings for the given city and institution
        DMMeetingsResponse meetingsResponse = DMServiceClient.checkoutMeetingsData(town.getName(), institution);

        DMImport.parseSeasons(town, institution, meetingsResponse.getSeasonDTOs());

        return town;
    }

}