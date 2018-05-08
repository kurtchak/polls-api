package org.blackbell.polls.source.base;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.source.DataImport;

import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 8.5.2018.
 */
public class BaseImport implements DataImport {
    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        return null;
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        return null;
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {

    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {

    }

    @Override
    public List<CouncilMember> loadMembers(Season season) {
        return null;
    }
}
