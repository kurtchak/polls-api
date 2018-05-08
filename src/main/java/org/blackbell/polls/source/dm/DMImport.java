package org.blackbell.polls.source.dm;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.source.DataImport;
import org.blackbell.polls.source.dm.api.DMServiceClient;

import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 25.2.2018.
 */
public class DMImport implements DataImport {

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        return DMParser.parseSeasonsResponse(town, DMServiceClient.checkoutSeasonsData(town));
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        return DMParser.parseMeetingsResponse(town, season, institution, DMServiceClient.checkoutMeetingsData(town, institution.getType(), season.getName()));
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        DMParser.parseMeetingResponse(meeting, DMServiceClient.checkoutMeetingData(externalMeetingId));
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {
        DMParser.parsePollDetail(poll, membersMap, DMServiceClient.checkoutPollData(poll.getExtAgendaItemId(), poll.getExtPollRouteId()));
    }

    //TODO:
    @Override
    public List<CouncilMember> loadMembers(Season season) {
        return null;
    }

}
