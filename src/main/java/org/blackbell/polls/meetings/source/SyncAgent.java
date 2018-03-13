package org.blackbell.polls.meetings.source;

import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.data.repositories.*;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.*;
import org.blackbell.polls.meetings.source.crawler.PresovCouncilMemberCrawler;
import org.blackbell.polls.meetings.source.dm.DMImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by kurtcha on 25.2.2018.
 */
@Component
public class SyncAgent {
    private static final Logger log = LoggerFactory.getLogger(SyncAgent.class);

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private SeasonRepository seasonRepository;

    @Autowired
    private TownRepository townRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    private List<Town> towns;
    private Map<Season, Map<String, CouncilMember>> allMembersMap;
    private Map<String, Party> partiesMap;

    public SyncAgent() {}

    @Scheduled(fixedRate = 3600000, initialDelay = 10000)
    public void syncCouncilMembers() {
        System.out.println("syncCouncilMembers...");
        // AD-HOC
        Season season = seasonRepository.findByRef("2014-2018");
        List<Party> parties = partyRepository.findAll();
        partiesMap = new HashMap<>();
        for (Party party : parties) {
            partiesMap.put(party.getName(), party);
        }

        towns = townRepository.findAll();
        for (Town town : towns) {
            Map<String, CouncilMember> councilMembersMap = new HashMap<>();
            List<CouncilMember> councilMembers = councilMemberRepository.findBySeason(season);
            for (CouncilMember councilMember : councilMembers) {
                councilMembersMap.put(councilMember.getName(), councilMember);
            }
            if ("presov".equals(town.getRef())) {
                List<CouncilMember> newCouncilMembers = new PresovCouncilMemberCrawler().getCouncilMembers(season, partiesMap, councilMembersMap);
                if (councilMembers != null) {
                    councilMemberRepository.save(newCouncilMembers);
                    System.out.println("New CouncilMembers saved");
                } else {
                    System.out.println(String.format("No new CouncilMembers found for town '%s' and season '%s'", town.getName(), season.getName()));
                }
            }
        }
        System.out.println("Council Members Sync finished");
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 1000000)
    public void sync() {
        towns = townRepository.findAll();
        if (towns == null) {
            log.info("No town to sync");
        } else {
            for (Town town : towns) {
                syncSeasons(town);
                town.setLastSyncDate(new Date());
                townRepository.save(town);
            }
        }
    }

    //TODO: zaciatok a koniec volebneho obdobia nie je jasne definovany
    private void syncSeasons(Town town) {
        try {
            Date syncDate = new Date();
            DataImport dataImport = getDataImport(town);
            List<Season> seasons = dataImport.loadSeasons(town);
            List<Season> formerSeasons = seasonRepository.findByTown(town.getName());
            for (Season season : seasons) {
                if (!formerSeasons.contains(season)) {
                    log.info("Adding new season for town %s: " + town.getName());
                    seasonRepository.save(season);
                }
                if (season.getLastSyncDate() == null || season.getLastSyncDate().before(syncDate)) {
                    syncMeetings(season);
                }
            }
        } catch (Exception e) {
            log.error("An error occured during the %ss seasons synchronization.", town.getName());
            e.printStackTrace();
        }

    }

    private void syncMeetings(Season season) {
        try {
            DataImport dataImport = getDataImport(season.getTown());
            System.out.println(season.getTown().getName() + " - " + season.getName() + " - " + season.getInstitution() + ":: LatestSyncDate: " + season.getLastSyncDate());
            List<Meeting> meetings = dataImport.loadMeetings(season);
            for (Meeting meeting : meetings) {
               if (season.getLastSyncDate() == null || meeting.getDate().after(season.getLastSyncDate())) {
                   loadMeeting(meeting, dataImport);
                   meetings.add(meeting);
                   season.setLastSyncDate(meeting.getDate());
               } else {
                   System.out.println(": meeting details already loaded: " + meeting.getDate());
               }
            }
            if (!meetings.isEmpty()) {
                seasonRepository.save(season);
                meetingRepository.save(meetings);// TODO: check synchronization with other meetings and its retention
            }
        } catch (Exception e) {
            log.error("An error occured during the %s meetings synchronization.", season.getRef());
            e.printStackTrace();
        }

    }

    private Meeting loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            for (AgendaItem item : meeting.getAgendaItems()) {
                for (Poll poll : item.getPolls()) {
                    dataImport.loadPollDetails(poll, getMembersMap(meeting.getSeason()));
                }
            }
        }
        System.out.println("NEW MEETING: " + meeting);
        return meeting;
    }

    private static DataImport getDataImport(Town town) {
        switch(town.getSource()) {
            case DM:
            default:
                return new DMImport();
        }
    }

    // MEMBERS
    public void addMember(Season season, CouncilMember member) {
        if (allMembersMap == null) {
            allMembersMap = new HashMap<>();
        }
        if (!allMembersMap.containsKey(season)) {
            allMembersMap.put(season, new HashMap<>());
        }
        allMembersMap.get(season).put(member.getName(), member);
    }

    public CouncilMember getMember(Season season, String name) {
        if (allMembersMap == null || !allMembersMap.containsKey(season)) {
            return null;
        }
        return allMembersMap.get(season).get(name);
    }

    public Collection<CouncilMember> getMembers(Season season) {
        return allMembersMap != null && allMembersMap.containsKey(season) ? allMembersMap.get(season).values() : null;
    }

    public void addMembers(Season season, List<CouncilMember> members) {
        if (allMembersMap == null) {
            allMembersMap = new HashMap<>();
        }
        if (!allMembersMap.containsKey(season)) {
            allMembersMap.put(season, new HashMap<>());
        }
        for (CouncilMember member : members) {
            allMembersMap.get(season).put(member.getName(), member);
        }
    }

    public Map<Season, Map<String, CouncilMember>> getAllMembersMap() {
        return allMembersMap;
    }

    public Map<String, CouncilMember> getMembersMap(Season season) {
        return allMembersMap != null ? allMembersMap.get(season) : null;
    }

}
