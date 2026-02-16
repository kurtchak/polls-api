package org.blackbell.polls.source.dm;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.source.DataImport;
import org.blackbell.polls.source.dm.api.DMServiceClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 25.2.2018.
 */
@Component
public class DMImport implements DataImport {

    private final DMServiceClient dmServiceClient;

    public DMImport(DMServiceClient dmServiceClient) {
        this.dmServiceClient = dmServiceClient;
    }

    @Override
    public List<Season> loadSeasons(Town town) throws Exception {
        return DMParser.parseSeasonsResponse(dmServiceClient.checkoutSeasonsData(town));
    }

    @Override
    public List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception {
        return DMParser.parseMeetingsResponse(town, season, institution, dmServiceClient.checkoutMeetingsData(town, institution.getType(), season.getName()));
    }

    @Override
    public void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception {
        DMParser.parseMeetingResponse(meeting, dmServiceClient.checkoutMeetingData(externalMeetingId));
    }

    @Override
    public void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception {
        DMParser.parsePollDetail(poll, membersMap, dmServiceClient.checkoutPollData(poll.getExtAgendaItemId(), poll.getExtPollRouteId()));
    }

    @Override
    public List<CouncilMember> loadMembers(Town town, Season season, Institution institution) {
        return null;
    }
}
