package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.*;
import org.blackbell.polls.source.base.BaseImport;
import org.blackbell.polls.source.crawler.PresovCouncilMemberCrawler;
import org.blackbell.polls.source.dm.DMImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private InstitutionRepository institutionRepository;

    private List<Town> towns;
    private Map<String, Map<String, CouncilMember>> allMembersMap;
    private Map<String, Party> partiesMap;
    private Map<InstitutionType, Institution> institutionsMap;

    public SyncAgent() {}

//    @Scheduled(fixedRate = 86400000, initialDelay = 60000)
    public void syncCouncilMembers() {
        log.info(Constants.MarkerSync, "syncCouncilMembers...");
        // AD-HOC
        Institution townCouncil = institutionRepository.findByType(InstitutionType.ZASTUPITELSTVO);
        Season season = seasonRepository.findByRef("2014-2018"); //TODO: all seasons
        List<Party> parties = partyRepository.findAll();
        partiesMap = new HashMap<>();
        for (Party party : parties) {
            partiesMap.put(party.getName(), party);
        }

        towns = townRepository.findAll();
        for (Town town : towns) {
            if ("presov".equals(town.getRef())) {
                Map<String, CouncilMember> councilMembersMap = new HashMap<>();
                Set<CouncilMember> councilMembers = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), InstitutionType.ZASTUPITELSTVO);
                for (CouncilMember councilMember : councilMembers) {
                    log.info("LOAD MEMBER: {}", councilMember.getPolitician().getName());
                    councilMembersMap.put(PollsUtils.getSimpleName(councilMember.getPolitician().getName()), councilMember);
                }
                Set<CouncilMember> newCouncilMembers =
                        new PresovCouncilMemberCrawler()
                                .getCouncilMembers(town, townCouncil, season, partiesMap, councilMembersMap);
                if (newCouncilMembers != null) {
                    log.info("NEW COUNCIL MEMBERS COUNT: {}", newCouncilMembers != null ? newCouncilMembers.size() : 0) ;
                    for (CouncilMember cm : newCouncilMembers) {
                        log.info("NEW COUNCIL MEMBER: {}", cm.getPolitician().getName());
                        councilMemberRepository.save(cm);
                        councilMemberRepository.flush();
                    }
                    log.info("New CouncilMembers saved");
                } else {
                    log.info(String.format("No new CouncilMembers found for town '%s' and season '%s'", town.getName(), season.getName()));
                }
            }
        }
        log.info(Constants.MarkerSync, "Council Members Sync finished");
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 10000)
//    @Transactional
    public void sync() {
        syncCouncilMembers();

        log.info(Constants.MarkerSync, "Synchronization started");
        towns = townRepository.findAll();
        institutionsMap = loadInstitutionsMap(institutionRepository.findAll());
        if (towns == null) {
            log.info(Constants.MarkerSync, "No town to sync");
        } else {
            for (Town town : towns) {
                syncSeasonMeetings(town);
                town.setLastSyncDate(new Date());
                townRepository.save(town);
            }
        }
    }

    private static Map<InstitutionType, Institution> loadInstitutionsMap(List<Institution> institutions) {
        Map<InstitutionType, Institution> institutionsMap = new HashMap<>();
        for (Institution institution : institutions) {
            institutionsMap.put(institution.getType(), institution);
        }
        return institutionsMap;
    }

    //TODO: zaciatok a koniec volebneho obdobia nie je jasne definovany
    private void syncSeasonMeetings(Town town) {
        log.info(Constants.MarkerSync, town.getName() + ": syncSeasonMeetings");
        try {
            Date syncDate = new Date();
            DataImport dataImport = getDataImport(town);
            List<Season> retrievedSeasons = dataImport.loadSeasons(town);
            if (retrievedSeasons != null) {
                for (Season retrievedSeason : retrievedSeasons) {
                    log.info("NEW SEASON: " + retrievedSeason);
                }
                log.info("Rertieved seasons: " + (retrievedSeasons != null ? retrievedSeasons.size() : 0));
            }
            List<Season> formerSeasons = seasonRepository.findAll();
            log.info(Constants.MarkerSync, "FormerSeasons: " + formerSeasons);
            for (Season retrievedSeason : formerSeasons) {
                log.info("retrievedSeason: " + retrievedSeason.hashCode());
                log.info("formerSeasons: " + formerSeasons.get(0).hashCode());
                if (!formerSeasons.contains(retrievedSeason)) {
                    log.info(String.format("Adding new season for town %s",town.getName()));
                    seasonRepository.save(retrievedSeason);
                }
                // load saved instance
                Season season = formerSeasons.get(formerSeasons.indexOf(retrievedSeason));
                log.info(Constants.MarkerSync, "Loaded Season to sync: " + season.getName());
                for (InstitutionType institutionType : InstitutionType.values()) {
                    syncSeasonMeetings(town, season, institutionsMap.get(institutionType));
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, String.format("An error occured during the %ss seasons synchronization.", town.getName()));
            e.printStackTrace();
        }

    }

    private void syncSeasonMeetings(Town town, Season season, Institution institution) {
        if (InstitutionType.KOMISIA.equals(institution.getType())) {
            //TODO:
            return;
        }
        log.info(Constants.MarkerSync, town.getName() + ":" + season.getName() + ":" + institution.getName() + ": syncSeasonMeetings");
        try {
            DataImport dataImport = getDataImport(town);
            Date latestMeetingDate = meetingRepository.getLatestMeetingDate(town, institution.getType(), season.getName());
            log.info(Constants.MarkerSync, ": LatestSyncDate: " + latestMeetingDate);
            List<Meeting> meetings = dataImport.loadMeetings(town, season, institution);
            if (meetings != null) {
                for (Meeting meeting : meetings) {
                    if (latestMeetingDate == null || meeting.getDate().after(latestMeetingDate)) {
                        loadMeeting(meeting, dataImport);
                        meetingRepository.save(meeting);// TODO: check synchronization with other meetings and its retention
                    } else {
                        log.info(Constants.MarkerSync, ": meeting details already loaded: " + meeting.getDate());
                    }
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, String.format("An error occured during the %s meetings synchronization.", season.getRef()));
            e.printStackTrace();
        }

    }

    private Meeting loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        log.info(Constants.MarkerSync, "loadMeeting: " + meeting);
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            for (AgendaItem item : meeting.getAgendaItems()) {
                if (item.getPolls() != null) {
                    for (Poll poll : item.getPolls()) {
                        log.info(Constants.MarkerSync, ">> poll: " + poll);
                        dataImport.loadPollDetails(poll, getMembersMap(meeting.getTown(), meeting.getSeason(), meeting.getInstitution()));
//                        for (Vote vote : poll.getVotes()) {
//                            log.info(Constants.MarkerSync, ">> vote: " + vote);
//                        }
                    }
                }
            }
        }
        log.info(Constants.MarkerSync, "NEW MEETING: " + meeting);
        return meeting;
    }

    private static DataImport getDataImport(Town town) {
        switch(town.getSource()) {
            case DM:
                return new DMImport();
            default:
                return new BaseImport();
        }
    }

    // MEMBERS
    public Map<String, CouncilMember> getMembersMap(Town town, Season season, Institution institution) throws Exception {
        log.info(Constants.MarkerSync, "Get membersMap for " + town.getName() + ":" + season.getName() + ":" + institution.getName());
        if (allMembersMap == null) {
            allMembersMap = new HashMap<>();
        }
        String membersKey = PollsUtils.generateMemberKey(town, season, institution.getType());
        if (!allMembersMap.containsKey(membersKey)) {
            loadCouncilMembers(town, season, institution);
        }
        return allMembersMap.get(membersKey);
    }

    private void loadCouncilMembers(Town town, Season season, Institution institution) throws Exception {
        log.info(Constants.MarkerSync, " -- loadCouncilMembers for season: " + season);
        Map<String, CouncilMember> membersMap = new HashMap<>();
        Set<CouncilMember> members = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), institution.getType());
        log.info(Constants.MarkerSync, " -- members: " + (members != null ? members.size() : 0));
        if (members == null || members.isEmpty()) { // TODO: preco vracia empty set?
            syncCouncilMembers();
            members = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), institution.getType());
            log.info(Constants.MarkerSync, " -- members: " + (members != null ? members.size() : 0));
        }
        if (members == null || members.isEmpty()) {
            throw new Exception(String.format("No CouncilMembers loaded for the town %s, season %s and institution %s.", town.getRef(), season.getRef(), institution));
        }
        for (CouncilMember councilMember : members) {
            log.info(Constants.MarkerSync, "Loaded Council Member > " + councilMember.getPolitician().getName());
            membersMap.put(PollsUtils.getSimpleName(councilMember.getPolitician().getName()), councilMember);
        }
        String membersKey = PollsUtils.generateMemberKey(town, season, institution.getType());
        log.info(Constants.MarkerSync, "Loaded Council Member Group > " + membersKey);
        allMembersMap.put(membersKey, membersMap);
    }

}
