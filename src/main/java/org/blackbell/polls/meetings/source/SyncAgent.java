package org.blackbell.polls.meetings.source;

import org.blackbell.polls.DataContext;
import org.blackbell.polls.common.PollDateUtils;
import org.blackbell.polls.data.repositories.MeetingRepository;
import org.blackbell.polls.data.repositories.SeasonRepository;
import org.blackbell.polls.data.repositories.TownRepository;
import org.blackbell.polls.meetings.dm.DMImportOld;
import org.blackbell.polls.meetings.dm.DMMeetingsResponse;
import org.blackbell.polls.meetings.dm.api.DMServiceClient;
import org.blackbell.polls.meetings.dm.dto.MeetingDTO;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Meeting;
import org.blackbell.polls.meetings.model.Season;
import org.blackbell.polls.meetings.model.Town;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    private Town getTown(String townName) {
        if (DataContext.getTowns() == null) {
            DataContext.addTowns(townRepository.findAll());
        }
        return DataContext.getTown(townName);
    }

    public void syncSeasons() {
        //TODO:
//        List<Season> seasons = dataImport.loadSeasons();

    }

    public void syncMeetings(String townName, String institution, String season) {
        try {
            Town town = getTown(townName);
            System.out.println("TOWN: " + town);
            DataImport dataImport = getDataImport(town);
            Date latestSyncDate = meetingRepository.getLatestMeetingDate(town, Institution.valueOfDM(institution), season);
            DMMeetingsResponse response = DMServiceClient.checkoutMeetingsData(town, Institution.valueOfDM(institution), season);
            for (MeetingDTO meetingDTO : response.getSeasonDTOs().get(0).getMeetingDTOs()) {
                if (!PollDateUtils.parseDMDate(meetingDTO.getDate()).before(latestSyncDate)) {
                    Meeting meeting = new Meeting();
                    meeting.setName(meetingDTO.getName());
                    meeting.setDate(PollDateUtils.parseDMDate(meetingDTO.getDate()));
                    meeting.setRef(DMImportOld.generateUniqueKeyReference()); // TODO:
                    meeting.setSeason(seasonRepository.findByRef(season));
                    meeting = dataImport.loadMeeting(meeting, meetingDTO.getId());
                    System.out.println("NEW MEETING: " + meeting);
                    meetingRepository.save(meeting);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static DataImport getDataImport(Town town) {
        System.out.println("TOWN.SOURCE: " + town.getSource());
        switch(town.getSource()) {
            case DM:
                return new DMImport();
            default:
                return null;
        }
    }
}
