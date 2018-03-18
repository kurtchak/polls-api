package org.blackbell.polls.meetings.source;

import org.blackbell.polls.meetings.model.*;

import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 25.2.2018.
 */
public interface DataImport {

    List<Season> loadSeasons(Town town) throws Exception;

    List<Meeting> loadMeetings(Season season) throws Exception;

    void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception;

    void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception;

    List<CouncilMember> loadMembers(Season season);

}
