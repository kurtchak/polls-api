package org.blackbell.polls.meetings.source.dm;

import org.blackbell.polls.DataContext;
import org.blackbell.polls.data.repositories.*;
import org.blackbell.polls.meetings.source.DataImport;
import org.blackbell.polls.meetings.source.dm.api.DMServiceClient;
import org.blackbell.polls.meetings.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by kurtcha on 25.2.2018.
 */
public class DMImport implements DataImport {

    @Autowired
    private TownRepository townRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    @Override
    public Town loadTown() {
        return null;
    }

    @Override
    public List<Season> loadSeasons() {
        return null;
    }

    @Override
    public List<Meeting> loadMeetings(Institution institution, Season season) {
        return null;
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        DMParser.parseMeetingResponse(meeting, DMServiceClient.checkoutMeetingData(externalMeetingId));
    }

    @Override
    public void loadPollDetails(Season season, Poll poll) throws Exception {
        DMParser.parsePollDetail(season, poll, DataContext.getMembersMap(season), DMServiceClient.checkoutPollData(poll.getExtAgendaItemId(), poll.getExtPollRouteId()));
    }

    @Override
    public CouncilMember loadMembers(Institution institution, Season season) {
        return null;
    }

    @Override
    public void syncSeasons() {

    }

}
