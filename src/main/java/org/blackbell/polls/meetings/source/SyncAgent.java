package org.blackbell.polls.meetings.source;

import org.blackbell.polls.DataContext;
import org.blackbell.polls.common.PollDateUtils;
import org.blackbell.polls.data.repositories.MeetingRepository;
import org.blackbell.polls.data.repositories.SeasonRepository;
import org.blackbell.polls.data.repositories.TownRepository;
import org.blackbell.polls.meetings.source.dm.DMImport;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingsResponse;
import org.blackbell.polls.meetings.source.dm.api.DMServiceClient;
import org.blackbell.polls.meetings.source.dm.dto.MeetingDTO;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Meeting;
import org.blackbell.polls.meetings.model.Season;
import org.blackbell.polls.meetings.model.Town;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kurtcha on 25.2.2018.
 */
@Service
public class SyncAgent {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private TownRepository townRepository;

    public SyncAgent() {}

    public static String generateUniqueKeyReference() {
        return "" + System.nanoTime();
    }

    private Town getTown(String townName) {
        if (DataContext.getTowns() == null) {
            DataContext.addTowns(townRepository.findAll());
        }
        return DataContext.getTown(townName);
    }

    public void syncSeasons(Town town) {
        //TODO:
        DataImport dataImport = getDataImport(town);
        List<Season> seasons = dataImport.loadSeasons();

    }

    public void syncMeetings(String townName, String institution, String seasonName) {
        try {
            Town town = getTown(townName);
            Season season = seasonRepository.findByRef(seasonName);
            DataImport dataImport = getDataImport(town);
            System.out.println(":: LatestSyncDate: " + town.getLastSyncDate());
            DMMeetingsResponse response = DMServiceClient.checkoutMeetingsData(town, Institution.valueOfDM(institution), seasonName);
            List<Meeting> meetings = new ArrayList<>();
            for (MeetingDTO meetingDTO : response.getSeasonDTOs().get(0).getMeetingDTOs()) {
                if (town.getLastSyncDate() == null || PollDateUtils.parseDMDate(meetingDTO.getDate()).after(town.getLastSyncDate())) {
                    Meeting meeting = new Meeting();
                    meeting.setName(meetingDTO.getName());
                    meeting.setDate(PollDateUtils.parseDMDate(meetingDTO.getDate()));
                    meeting.setRef(generateUniqueKeyReference()); // TODO:
                    meeting.setSeason(season);
                    meeting = dataImport.loadMeeting(meeting, meetingDTO.getId());
                    System.out.println("NEW MEETING: " + meeting);
                    meetings.add(meeting);
                    return;
//                    town.setLastSyncDate(meeting.getDate());
                } else {
                    System.out.println(": meeting already loaded: " + meetingDTO.getDate());
                }
            }
//            townRepository.save(town);
//            meetingRepository.save(meetings);// TODO: check synchronization with other meetings and its retention
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static DataImport getDataImport(Town town) {
        switch(town.getSource()) {
            case DM:
            default:
                return new DMImport();
        }
    }
}
