package org.blackbell.polls.domain;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.Source;

import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 25.2.2018.
 */
public interface DataImport {

    Source getSource();

    List<Season> loadSeasons(Town town) throws Exception;

    List<Meeting> loadMeetings(Town town, Season season, Institution institution) throws Exception;

    void loadMeetingDetails(Meeting meeting, String externalMeetingId) throws Exception;

    void loadPollDetails(Poll poll, Map<String, CouncilMember> membersMap) throws Exception;

    List<CouncilMember> loadMembers(Town town, Season season, Institution institution);

}
