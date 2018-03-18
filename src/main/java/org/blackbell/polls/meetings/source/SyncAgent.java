package org.blackbell.polls.meetings.source;

import org.blackbell.polls.data.repositories.*;
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

    @Scheduled(fixedRate = 86400000, initialDelay = 60000)
    public void syncCouncilMembers() {
        log.info("syncCouncilMembers...");
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
                    log.info("New CouncilMembers saved");
                } else {
                    log.info(String.format("No new CouncilMembers found for town '%s' and season '%s'", town.getName(), season.getName()));
                }
            }
        }
        log.info("Council Members Sync finished");
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 600000)
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
            List<Season> retrievedSeasons = dataImport.loadSeasons(town);
            for (Season retrievedSeason : retrievedSeasons) {
                log.info("NEW SEASON: " + retrievedSeason);
            }
//            log.info("Rertieved seasons: " + (retrievedSeasons != null ? retrievedSeasons.size() : 0));
            List<Season> formerSeasons = seasonRepository.findByTown(town);
//            log.info("FormerSeasons: " + formerSeasons);
            for (Season retrievedSeason : retrievedSeasons) {
                if (!Institution.ZASTUPITELSTVO.equals(retrievedSeason.getInstitution())) {
                    continue;
                }
//                log.info("retrievedSeason: " + retrievedSeason.hashCode());
//                log.info("formerSeasons: " + formerSeasons.get(0).hashCode());
                if (!formerSeasons.contains(retrievedSeason)) {
                    log.info(String.format("Adding new season for town %s",town.getName()));
                    seasonRepository.save(retrievedSeason);
                }
                // load saved instance
                Season season = formerSeasons.get(formerSeasons.indexOf(retrievedSeason));
                syncMeetings(season);
            }
        } catch (Exception e) {
            log.error(String.format("An error occured during the %ss seasons synchronization.", town.getName()));
            e.printStackTrace();
        }

    }

    private void syncMeetings(Season season) {
        log.info("syncMeetings for season: " + season.getName());
        try {
            DataImport dataImport = getDataImport(season.getTown());
            log.info(season.getTown().getName() + " - " + season.getName() + " - " + season.getInstitution() + ":: LatestSyncDate: " + season.getLastSyncDate());
            List<Meeting> meetings = dataImport.loadMeetings(season);
            List<Meeting> newMeetings = new ArrayList<>();
            for (Meeting meeting : meetings) {
               if (season.getLastSyncDate() == null || meeting.getDate().after(season.getLastSyncDate())) {
                   loadMeeting(meeting, dataImport);
//                   newMeetings.add(meeting);
                   meetingRepository.save(meeting);// TODO: check synchronization with other meetings and its retention
                   season.setLastSyncDate(meeting.getDate()); // set now()
                   seasonRepository.save(season);
               } else {
                   log.info(": meeting details already loaded: " + meeting.getDate());
               }
            }
//            if (!newMeetings.isEmpty()) {
//                meetingRepository.save(newMeetings);// TODO: check synchronization with other meetings and its retention
//            }
//            season.setLastSyncDate(new Date()); // set now()
//            seasonRepository.save(season);
        } catch (Exception e) {
            log.error(String.format("An error occured during the %s meetings synchronization.", season.getRef()));
            e.printStackTrace();
        }

    }

    private Meeting loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        log.info("loadMeeting: " + meeting);
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            for (AgendaItem item : meeting.getAgendaItems()) {
                if (item.getPolls() != null) {
                    for (Poll poll : item.getPolls()) {
                        log.info(">> poll: " + poll);
                        dataImport.loadPollDetails(poll, getMembersMap(meeting.getSeason()));
                    }
                }
            }
        }
        log.info("NEW MEETING: " + meeting);
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
    public Map<String, CouncilMember> getMembersMap(Season season) throws Exception {
        if (allMembersMap == null) {
            allMembersMap = new HashMap<>();
        }
        if (!allMembersMap.containsKey(season)) {
            loadCouncilMembers(season);
        }
        return allMembersMap.get(season);
    }

    private void loadCouncilMembers(Season season) throws Exception {
        log.info(" -- loadCouncilMembers for season: " + season);
        Map<String, CouncilMember> membersMap = new HashMap<>();
        List<CouncilMember> seasonMembers = councilMemberRepository.findBySeason(season);
        log.info(" -- seasonMembers: " + (seasonMembers != null ? seasonMembers.size() : 0));
        if (seasonMembers == null || seasonMembers.isEmpty()) { // TODO: preco vracia empty set?
            syncCouncilMembers();
        }
        seasonMembers = councilMemberRepository.findBySeason(season);
        log.info(" -- seasonMembers: " + (seasonMembers != null ? seasonMembers.size() : 0));
        if (seasonMembers == null || seasonMembers.isEmpty()) {
            throw new Exception(String.format("No CouncilMembers loaded for the town %s, season %s and institution %s.", season.getTown().getRef(), season.getRef(), season.getInstitution()));
        }
        for (CouncilMember councilMember : seasonMembers) {
            log.info("New Council Member > " + councilMember.getName());
            membersMap.put(councilMember.getName(), councilMember);
        }
        allMembersMap.put(season, membersMap);
    }

}
