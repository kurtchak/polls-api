package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.*;
import org.blackbell.polls.source.base.BaseImport;
import org.blackbell.polls.source.bratislava.BratislavaImport;
import org.blackbell.polls.source.crawler.PresovCouncilMemberCrawlerV2;
import org.blackbell.polls.source.dm.DMImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;
import static org.blackbell.polls.common.PollsUtils.toSimpleNameWithoutAccents;

/**
 * Created by kurtcha on 25.2.2018.
 */
@Component
public class SyncAgent {
    private static final Logger log = LoggerFactory.getLogger(SyncAgent.class);

    private static final String PRESOV_REF = "presov";

    private final MeetingRepository meetingRepository;
    private final SeasonRepository seasonRepository;
    private final TownRepository townRepository;
    private final PartyRepository partyRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final InstitutionRepository institutionRepository;
    private final ClubRepository clubRepository;
    private final PoliticianRepository politicianRepository;
    private final SyncAgent self; // Self-injection for transactional method calls

    private Map<String, Town> townsMap;
    private Map<String, Map<String, CouncilMember>> allMembersMap;
    private Map<String, Party> partiesMap;
    private Map<InstitutionType, List<Institution>> institutionsMap;
    private Map<String, Season> seasonsMap;
    private Map<String, Politician> politiciansMap; // For tracking politicians across seasons

    public SyncAgent(MeetingRepository meetingRepository, SeasonRepository seasonRepository,
                     TownRepository townRepository, PartyRepository partyRepository,
                     CouncilMemberRepository councilMemberRepository, InstitutionRepository institutionRepository,
                     ClubRepository clubRepository, PoliticianRepository politicianRepository,
                     @Lazy SyncAgent self) {
        this.meetingRepository = meetingRepository;
        this.seasonRepository = seasonRepository;
        this.townRepository = townRepository;
        this.partyRepository = partyRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.institutionRepository = institutionRepository;
        this.clubRepository = clubRepository;
        this.politicianRepository = politicianRepository;
        this.self = self;
    }

    @Transactional
    public void syncCouncilMembers(Town town) {
        log.info(Constants.MarkerSync, "syncCouncilMembers...");
        Institution townCouncil = institutionRepository.findByType(InstitutionType.ZASTUPITELSTVO);

        // Load existing politicians for reuse across seasons (tracking "prezliekači")
        loadPoliticiansMap(town);

        for (String seasonRef : getSeasonsRefs()) {
            if (PRESOV_REF.equals(town.getRef())) {
                Set<CouncilMember> councilMembers = councilMemberRepository
                        .getByTownAndSeasonAndInstitution(
                                town.getRef(),
                                seasonRef,
                                InstitutionType.ZASTUPITELSTVO);

                Map<String, CouncilMember> councilMembersMap = councilMembers
                        .stream().collect(
                                Collectors.toMap(
                                        cm -> toSimpleNameWithoutAccents(cm.getPolitician().getName()),
                                        cm -> cm));

                log.info("COUNCIL MEMBERS for season {}: {}", seasonRef, councilMembers.size());

                if (councilMembers.isEmpty()) {
                    PresovCouncilMemberCrawlerV2 crawler = new PresovCouncilMemberCrawlerV2();
                    Set<CouncilMember> newCouncilMembers =
                            crawler.getCouncilMembers(town, townCouncil, getSeason(seasonRef), getPartiesMap(), councilMembersMap);

                    if (newCouncilMembers != null && !newCouncilMembers.isEmpty()) {
                        // Reuse existing politicians across seasons
                        for (CouncilMember cm : newCouncilMembers) {
                            String politicianKey = toSimpleNameWithoutAccents(cm.getPolitician().getName());
                            Politician existingPolitician = politiciansMap.get(politicianKey);

                            if (existingPolitician != null) {
                                // Reuse existing politician - this tracks the person across seasons
                                log.info("Reusing existing politician: {} (ID: {})",
                                        PollsUtils.deAccent(existingPolitician.getName()), existingPolitician.getId());

                                // Update contact info if newer
                                if (cm.getPolitician().getEmail() != null) {
                                    existingPolitician.setEmail(cm.getPolitician().getEmail());
                                }
                                if (cm.getPolitician().getPhone() != null) {
                                    existingPolitician.setPhone(cm.getPolitician().getPhone());
                                }
                                if (cm.getPolitician().getPicture() != null) {
                                    existingPolitician.setPicture(cm.getPolitician().getPicture());
                                }

                                // Transfer party nominees to existing politician
                                if (cm.getPolitician().getPartyNominees() != null) {
                                    for (var nominee : cm.getPolitician().getPartyNominees()) {
                                        nominee.setPolitician(existingPolitician);
                                        existingPolitician.addPartyNominee(nominee);
                                    }
                                }

                                cm.setPolitician(existingPolitician);
                            } else {
                                // New politician
                                log.info("NEW POLITICIAN: {}", PollsUtils.deAccent(cm.getPolitician().getName()));
                                politiciansMap.put(politicianKey, cm.getPolitician());
                            }

                            log.info("NEW COUNCIL MEMBER: {} (season: {})",
                                    PollsUtils.deAccent(cm.getPolitician().getName()), seasonRef);
                        }

                        // Save clubs created by crawler
                        Map<String, Club> clubs = crawler.getClubsMap();
                        if (!clubs.isEmpty()) {
                            log.info("Saving {} clubs", clubs.size());
                            clubRepository.saveAll(clubs.values());
                        }

                        councilMemberRepository.saveAll(newCouncilMembers);
                    } else {
                        log.info("No new CouncilMembers found for town {} and season {}", town.getName(), seasonRef);
                    }
                }
            }
        }
        log.info(Constants.MarkerSync, "Council Members Sync finished");
    }

    /**
     * Load existing politicians for the town to enable reuse across seasons.
     * This is key for tracking "prezliekači" - politicians who change parties/clubs.
     */
    private void loadPoliticiansMap(Town town) {
        if (politiciansMap == null) {
            politiciansMap = new HashMap<>();
        }
        List<Politician> existingPoliticians = politicianRepository.findByTown(town.getRef());
        for (Politician p : existingPoliticians) {
            String key = toSimpleNameWithoutAccents(p.getName());
            politiciansMap.put(key, p);
        }
        log.info("Loaded {} existing politicians for town {}", politiciansMap.size(), town.getName());
    }

    private Map<String, Party> getPartiesMap() {
        if (partiesMap == null) {
            loadPartiesMap();
        }
        return partiesMap;
    }

    private Town getTown(String ref) {
        if (townsMap == null) {
            loadTownsMap();
        }
        return townsMap.get(ref);
    }

    private Set<String> getTownsRefs() {
        if (townsMap == null) {
            loadTownsMap();
        }
        return townsMap.keySet();
    }

    private Season getSeason(String ref) {
        if (seasonsMap == null) {
            loadSeasonsMap();
        }
        return seasonsMap.get(ref);
    }

    private Set<String> getSeasonsRefs() {
        if (seasonsMap == null) {
            loadSeasonsMap();
        }
        return seasonsMap.keySet();
    }

    private void loadSeasonsMap() {
        seasonsMap = seasonRepository.findAll()
                .stream().collect(Collectors.toMap(Season::getRef, p -> p));
    }

    private void loadPartiesMap() {
        partiesMap = partyRepository.findAll()
                .stream().collect(Collectors.toMap(Party::getName, p -> p));
    }

    private void loadTownsMap() {
        townsMap = new HashMap<>();
        for (String ref : List.of("presov", "poprad")) {
            Town town = townRepository.findByRef(ref);
            if (town != null) {
                townsMap.put(ref, town);
            }
        }
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 5000)
    public void sync() {
        Set<String> townsRefs = getTownsRefs();
        institutionsMap = loadInstitutionsMap(institutionRepository.findAll());
        log.info(Constants.MarkerSync, "Synchronization started");

        if (townsRefs.isEmpty()) {
            log.info(Constants.MarkerSync, "No town to sync");
        }

        townsRefs.forEach(townRef -> {
            log.info("town: {}", townRef);
            Town town = getTown(townRef);
            syncSeasons(town);
            self.syncCouncilMembers(town);

            getSeasonsRefs().forEach(seasonRef -> syncSeasonMeetings(town, getSeason(seasonRef)));
            self.saveTownLastSyncDate(town);
        });
    }

    private void syncSeasons(Town town) {
        try {
            List<Season> retrievedSeasons =
                    new ArrayList<>(getDataImport(town)
                            .loadSeasons(town));
            log.info(Constants.MarkerSync, "RETRIEVED SEASONS: {}", retrievedSeasons);

            // Load Former Seasons
            List<Season> formerSeasons = seasonRepository.findAll();
            log.info(Constants.MarkerSync, "FORMER SEASONS: {}", formerSeasons);

            // Save New Seasons
            retrievedSeasons.stream().filter(season -> !formerSeasons.contains(season)).forEach(self::saveNewSeason);
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {}s seasons synchronization.", town.getName());
            e.printStackTrace();
        }
    }

    private static Map<InstitutionType, List<Institution>> loadInstitutionsMap(List<Institution> institutions) {
        Map<InstitutionType, List<Institution>> institutionTypeListMap = new HashMap<>();
        for (Institution institution : institutions) {
            institutionTypeListMap.computeIfAbsent(institution.getType(), k -> new ArrayList<>());
            institutionTypeListMap.get(institution.getType()).add(institution);
        }
        return institutionTypeListMap;
    }

    //TODO: zaciatok a koniec volebneho obdobia nie je jasne definovany
    private void syncSeasonMeetings(Town town, Season season) {
        log.info(Constants.MarkerSync, "{}: syncSeasonMeetings", town.getName());
        // load saved instance
        log.info(Constants.MarkerSync, "Loaded Season to sync: {}", season);

        institutionsMap.keySet()
                .forEach(type -> self.syncSeasonMeetings(town, season, institutionsMap.get(type).get(0))); // AD-HOC - just first Institution-Comission is retrieved

    }

    @Transactional
    public void saveNewSeason(Season season) {
        log.info("Adding new season: {}", season);
        seasonRepository.save(season);
    }

    @Transactional
    public void saveTownLastSyncDate(Town town) {
        town.setLastSyncDate(new Date());
        townRepository.save(town);
        log.info("Updated lastSyncDate for town: {}", town.getName());
    }

    @Transactional
    public void syncSeasonMeetings(Town town, Season season, Institution institution) {
        log.info("syncSeasonMeetings -> {} - {} - {}", town, season, institution);
        if (InstitutionType.KOMISIA.equals(institution.getType())) {
            //TODO:
            return;
        }
        log.info(Constants.MarkerSync, "{}:{}:{}: syncSeasonMeetings", town.getName(), season.getName(), institution.getName());
        try {
            DataImport dataImport = getDataImport(town);
            List<Meeting> meetings = dataImport.loadMeetings(town, season, institution);
            if (meetings != null) {
                for (Meeting meeting : meetings) {
                    Meeting existing = meetingRepository.findByExtId(meeting.getExtId());
                    if (existing != null) {
                        if (existing.getAgendaItems() != null && !existing.getAgendaItems().isEmpty()) {
                            log.info(Constants.MarkerSync, "Meeting already loaded with {} agenda items: {}",
                                    existing.getAgendaItems().size(), meeting.getName());
                            continue;
                        }
                        // Delete incomplete meeting (saved without agenda items due to earlier errors)
                        log.info(Constants.MarkerSync, "Re-loading incomplete meeting (0 agenda items): {}", meeting.getName());
                        meetingRepository.delete(existing);
                        meetingRepository.flush();
                    }
                    loadMeeting(meeting, dataImport);
                    meetingRepository.save(meeting);
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, String.format("An error occured during the %s meetings synchronization.", season.getRef()));
            e.printStackTrace();
        }

    }

    private void loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        log.info(Constants.MarkerSync, "loadMeeting: {}", meeting);
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            for (AgendaItem item : meeting.getAgendaItems()) {
                if (item.getPolls() != null) {
                    for (Poll poll : item.getPolls()) {
                        log.info(Constants.MarkerSync, ">> poll: {}", poll);
                        try {
                            dataImport.loadPollDetails(poll, getMembersMap(meeting.getTown(), meeting.getSeason(), meeting.getInstitution()));
                        } catch (Exception e) {
                            log.warn(Constants.MarkerSync, "Could not load poll details (votes) for poll '{}' - saving poll with vote counts only: {}",
                                    poll.getName(), e.getMessage());
                        }
                    }
                }
            }
        }
        log.info(Constants.MarkerSync, "NEW MEETING: {}", meeting);
    }

    private static DataImport getDataImport(Town town) {
        if (town.getSource() == Source.DM) {
            return new DMImport();
        }
        if (town.getSource() == Source.BA_OPENDATA) {
            return new BratislavaImport();
        }
        return new BaseImport();
    }

    // MEMBERS
    private Map<String, CouncilMember> getMembersMap(Town town, Season season, Institution institution) throws Exception {
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
            self.syncCouncilMembers(town);
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
