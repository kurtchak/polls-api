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

    Meeting loadMeeting(Meeting meeting, String externalMeetingId);

    Poll loadPoll(String externalPollId);

    CouncilMember loadMembers(Institution institution, Season season);

    void syncSeasons();
}
