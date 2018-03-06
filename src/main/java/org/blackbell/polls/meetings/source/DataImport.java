package org.blackbell.polls.meetings.source;

import org.blackbell.polls.meetings.model.*;

import java.util.List;

/**
 * Created by kurtcha on 25.2.2018.
 */
public interface DataImport {

    Town loadTown();

    List<Season> loadSeasons();

    List<Meeting> loadMeetings(Institution institution, Season season);

    void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception;

    void loadPollDetails(Season season, Poll poll) throws Exception;

    CouncilMember loadMembers(Institution institution, Season season);

    void syncSeasons();
}
