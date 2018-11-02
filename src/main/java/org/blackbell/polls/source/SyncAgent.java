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
import java.util.stream.Collectors;

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

        partiesMap = partyRepository.findAll()
                .stream().collect(Collectors.toMap(p -> p.getName(), p -> p));
        towns = townRepository.findAll();

        for (Town town : towns) {
            if ("presov".equals(town.getRef())) {

                Set<CouncilMember> councilMembers = councilMemberRepository
                        .getByTownAndSeasonAndInstitution(
                                town.getRef(),
                                season.getRef(),
                                InstitutionType.ZASTUPITELSTVO);

                Map<String, CouncilMember> councilMembersMap = councilMembers
                        .stream().collect(
                                Collectors.toMap(
                                        cm -> PollsUtils
                                                .toSimpleNameWithoutAccents(
                                                        cm.getPolitician().getName()),
                                        cm -> cm));

                log.info("COUNCIL MEMBERS: {}", councilMembers != null ? councilMembers.size() : 0);

                Set<CouncilMember> newCouncilMembers =
                        new PresovCouncilMemberCrawler()
                                .getCouncilMembers(town, townCouncil, season, partiesMap, councilMembersMap);

                if (newCouncilMembers != null) {
                    // log
                    newCouncilMembers.stream().forEach(
                            cm -> log.info("NEW COUNCIL MEMBER: {}",
                                    PollsUtils.deAccent(cm.getPolitician().getName())));

                    councilMemberRepository.save(newCouncilMembers);
                } else {
                    log.info("No new CouncilMembers found for town {} and season {}", town.getName(), season.getName());
                }
            }
        }
        log.info(Constants.MarkerSync, "Council Members Sync finished");
//        councilMemberRepository.flush();
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 500)
    @Transactional
    public void sync() {
        towns = townRepository.findAll();
        institutionsMap = loadInstitutionsMap(institutionRepository.findAll());
        log.info(Constants.MarkerSync, "Synchronization started");

        syncCouncilMembers();

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
        return institutions.stream()
                .collect(Collectors.toMap(
                        institution -> institution.getType(),
                        institution -> institution));
    }

    //TODO: zaciatok a koniec volebneho obdobia nie je jasne definovany
    private void syncSeasonMeetings(Town town) {
        log.info(Constants.MarkerSync, "{}: syncSeasonMeetings", town.getName());
        try {
            // Load New Seasons
            List<Season> retrievedSeasons =
                    getDataImport(town)
                            .loadSeasons(town).stream().collect(Collectors.toList());

            retrievedSeasons.stream().forEach(season -> log.info("NEW SEASON: {}", season));

            // Load Former Seasons
            List<Season> formerSeasons = seasonRepository.findAll();
            log.info(Constants.MarkerSync, "FormerSeasons: {}", formerSeasons);

            // Save New Seasons
            formerSeasons.stream()
                    .filter(retrievedSeason -> formerSeasons.contains(retrievedSeason))
                    .forEach(retrievedSeason -> saveNewSeason(retrievedSeason));

            // Reload again all Seasons
            List<Season> seasons = seasonRepository.findAll();
            for (Season season : seasons) {

                // load saved instance
                log.info(Constants.MarkerSync, "Loaded Season to sync: {}", season);

                institutionsMap.keySet().stream()
                        .forEach(type -> syncSeasonMeetings(town, season, institutionsMap.get(type)));
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {}s seasons synchronization.", town.getName());
            e.printStackTrace();
        }

    }

    private void saveNewSeason(Season season) {
        log.info(String.format("Adding new season: {}", season));
        seasonRepository.save(season);
    }

    private void syncSeasonMeetings(Town town, Season season, Institution institution) {
        log.info("syncSeasonMeetings -> {} - {} - {}", town, season, institution);
        if (InstitutionType.KOMISIA.equals(institution.getType())) {
            //TODO:
            return;
        }
        log.info(Constants.MarkerSync, "{}:{}:{}: syncSeasonMeetings", town.getName(), season.getName(), institution.getName());
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
                        log.info(Constants.MarkerSync, ": meeting details already loaded: {}", meeting.getDate());
                    }
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, String.format("An error occured during the %s meetings synchronization.", season.getRef()));
            e.printStackTrace();
        }

    }

    private Meeting loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        log.info(Constants.MarkerSync, "loadMeeting: {}", meeting);
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            for (AgendaItem item : meeting.getAgendaItems()) {
                if (item.getPolls() != null) {
                    for (Poll poll : item.getPolls()) {
                        log.info(Constants.MarkerSync, ">> poll: {}", poll);
                        dataImport.loadPollDetails(poll, getMembersMap(meeting.getTown(), meeting.getSeason(), meeting.getInstitution()));
                    }
                }
            }
        }
        log.info(Constants.MarkerSync, "NEW MEETING: {}", meeting);
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
        log.info(Constants.MarkerSync, "Get membersMap for {}:{}:", town.getName(), institution.getName());
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
        log.info(Constants.MarkerSync, " -- loadCouncilMembers for season: {}", season);
        Map<String, CouncilMember> membersMap = new HashMap<>();
        Set<CouncilMember> members = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), institution.getType());
        log.info(Constants.MarkerSync, " -- members: {}", (members != null ? members.size() : 0));
        if (members == null || members.isEmpty()) { // TODO: preco vracia empty set?
            syncCouncilMembers();
            members = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), institution.getType());
            log.info(Constants.MarkerSync, " -- members: {}", (members != null ? members.size() : 0));
        }
        if (members == null || members.isEmpty()) {
            throw new Exception(String.format("No CouncilMembers loaded for the town %s, season %s and institution %s.", town.getRef(), season.getRef(), institution));
        }
        for (CouncilMember councilMember : members) {
            log.info(Constants.MarkerSync, "Loaded Council Member > {}", councilMember.getPolitician().getName());
            membersMap.put(PollsUtils.toSimpleNameWithoutAccents(councilMember.getPolitician().getName()), councilMember);
        }
        String membersKey = PollsUtils.generateMemberKey(town, season, institution.getType());
        log.info(Constants.MarkerSync, "Loaded Council Member Group > {}", membersKey);
        allMembersMap.put(membersKey, membersMap);
    }

}
