package org.blackbell.polls.meetings.source;

import org.blackbell.polls.data.repositories.*;
import org.blackbell.polls.meetings.dm.DMImportOld;
import org.blackbell.polls.meetings.dm.api.DMServiceClient;
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
    public Meeting loadMeeting(Meeting meeting, String externalMeetingId) {
        try {
            return DMImportOld.parseMeetingResponse(meeting, meeting.getSeason(), DMServiceClient.checkoutMeetingData(externalMeetingId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Poll loadPoll(String externalPollId) {
        return null;
    }

    @Override
    public CouncilMember loadMembers(Institution institution, Season season) {
        return null;
    }

    @Override
    public void syncSeasons() {

    }
}
