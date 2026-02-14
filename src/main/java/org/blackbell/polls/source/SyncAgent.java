package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.DataSourceType;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.*;
import org.blackbell.polls.source.dm.DMPdfImporter;
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

import org.blackbell.polls.sync.SyncProgress;

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
    private final SyncProgress syncProgress;

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
                     @Lazy SyncAgent self, SyncProgress syncProgress) {
        this.meetingRepository = meetingRepository;
        this.seasonRepository = seasonRepository;
        this.townRepository = townRepository;
        this.partyRepository = partyRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.institutionRepository = institutionRepository;
        this.clubRepository = clubRepository;
        this.politicianRepository = politicianRepository;
        this.self = self;
        this.syncProgress = syncProgress;
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
            } else if ("bratislava".equals(town.getRef())) {
                Set<CouncilMember> councilMembers = councilMemberRepository
                        .getByTownAndSeasonAndInstitution(
                                town.getRef(),
                                seasonRef,
                                InstitutionType.ZASTUPITELSTVO);

                log.info("BRATISLAVA COUNCIL MEMBERS for season {}: {}", seasonRef, councilMembers.size());

                if (councilMembers.isEmpty()) {
                    DataImport dataImport = getDataImport(town);
                    List<CouncilMember> newMembers = dataImport.loadMembers(getSeason(seasonRef));

                    if (newMembers != null && !newMembers.isEmpty()) {
                        for (CouncilMember cm : newMembers) {
                            cm.setTown(town);
                            cm.setInstitution(townCouncil);

                            String politicianKey = toSimpleNameWithoutAccents(cm.getPolitician().getName());
                            Politician existingPolitician = politiciansMap.get(politicianKey);

                            if (existingPolitician != null) {
                                log.info("Reusing existing politician: {} (ID: {})",
                                        PollsUtils.deAccent(existingPolitician.getName()), existingPolitician.getId());
                                cm.setPolitician(existingPolitician);
                            } else {
                                log.info("NEW POLITICIAN: {}", PollsUtils.deAccent(cm.getPolitician().getName()));
                                politiciansMap.put(politicianKey, cm.getPolitician());
                            }
                        }
                        councilMemberRepository.saveAll(newMembers);
                        log.info("Saved {} Bratislava council members for season {}", newMembers.size(), seasonRef);
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
        for (String ref : List.of("presov", "poprad", "bratislava")) {
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
        syncProgress.startSync();

        if (townsRefs.isEmpty()) {
            log.info(Constants.MarkerSync, "No town to sync");
        }

        try {
            townsRefs.forEach(townRef -> {
                log.info("town: {}", townRef);
                Town town = getTown(townRef);
                syncProgress.startTown(townRef);
                syncSeasons(town);
                self.syncCouncilMembers(town);

                getSeasonsRefs().forEach(seasonRef -> syncSeasonMeetings(town, getSeason(seasonRef)));
                self.saveTownLastSyncDate(town);
                log.info(Constants.MarkerSync, "Synchronization finished for town: {}", townRef);
            });
        } finally {
            syncProgress.finishSync();
            log.info(Constants.MarkerSync, "Synchronization finished");
        }
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

    public void syncSeasonMeetings(Town town, Season season, Institution institution) {
        log.debug("syncSeasonMeetings -> {} - {} - {}", town, season, institution);
        if (InstitutionType.KOMISIA.equals(institution.getType())) {
            //TODO:
            return;
        }
        log.info(Constants.MarkerSync, "{}:{}:{}: syncSeasonMeetings", town.getName(), season.getName(), institution.getName());
        try {
            DataImport dataImport = getDataImport(town);
            List<Meeting> meetings = dataImport.loadMeetings(town, season, institution);
            if (meetings != null) {
                syncProgress.startSeason(town.getRef(), season.getRef(), meetings.size());
                for (Meeting meeting : meetings) {
                    try {
                        self.syncSingleMeeting(meeting, dataImport);
                    } catch (Exception e) {
                        log.error(Constants.MarkerSync, "Failed to sync meeting '{}': {}", meeting.getName(), e.getMessage());
                    } finally {
                        syncProgress.meetingProcessed();
                    }
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, String.format("An error occured during the %s meetings synchronization.", season.getRef()));
            e.printStackTrace();
        }
    }

    @Transactional
    public void syncSingleMeeting(Meeting meeting, DataImport dataImport) {
        Meeting existing = meetingRepository.findByExtId(meeting.getExtId());
        if (existing != null) {
            if (existing.getSyncError() != null) {
                log.info(Constants.MarkerSync, "Retrying previously failed meeting: {} (error: {})",
                        meeting.getName(), existing.getSyncError());
            } else if (existing.getAgendaItems() != null && !existing.getAgendaItems().isEmpty()) {
                // Check if polls have individual votes WITH matched council members
                boolean hasVotes = existing.getAgendaItems().stream()
                        .filter(ai -> ai.getPolls() != null)
                        .flatMap(ai -> ai.getPolls().stream())
                        .anyMatch(p -> p.getVotes() != null && !p.getVotes().isEmpty());
                if (hasVotes) {
                    boolean hasUnmatchedVotes = existing.getAgendaItems().stream()
                            .filter(ai -> ai.getPolls() != null)
                            .flatMap(ai -> ai.getPolls().stream())
                            .filter(p -> p.getVotes() != null)
                            .flatMap(p -> p.getVotes().stream())
                            .anyMatch(v -> v.getCouncilMember() == null);
                    if (!hasUnmatchedVotes) {
                        log.debug(Constants.MarkerSync, "Meeting fully matched with {} agenda items: {}",
                                existing.getAgendaItems().size(), meeting.getName());
                        return;
                    }
                    log.info(Constants.MarkerSync, "Re-loading meeting with unmatched votes: {}", meeting.getName());
                } else {
                    log.info(Constants.MarkerSync, "Re-loading meeting without individual votes: {}", meeting.getName());
                }
            } else {
                log.info(Constants.MarkerSync, "Re-loading incomplete meeting (0 agenda items): {}", meeting.getName());
            }
            meetingRepository.delete(existing);
            meetingRepository.flush();
        }
        try {
            loadMeeting(meeting, dataImport);
            meeting.setSyncError(null);
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "Error loading meeting '{}': {}", meeting.getName(), e.getMessage());
            meeting.setSyncError(e.getMessage());
        }
        meetingRepository.save(meeting);
    }

    private void loadMeeting(Meeting meeting, DataImport dataImport) throws Exception {
        log.debug(Constants.MarkerSync, "loadMeeting: {}", meeting);
        dataImport.loadMeetingDetails(meeting, meeting.getExtId());
        if (meeting.getAgendaItems() != null) {
            Map<String, CouncilMember> membersMap = getMembersMap(meeting.getTown(), meeting.getSeason(), meeting.getInstitution());

            boolean hasPolls = meeting.getAgendaItems().stream()
                    .anyMatch(ai -> ai.getPolls() != null && !ai.getPolls().isEmpty());

            int attachmentCount = meeting.getAttachments() != null ? meeting.getAttachments().size() : 0;
            log.info(Constants.MarkerSync, "Meeting '{}': {} agenda items, hasPolls={}, {} attachments",
                    meeting.getName(), meeting.getAgendaItems().size(), hasPolls, attachmentCount);

            if (hasPolls) {
                // PRIORITY 1: DM API structured data
                for (AgendaItem item : meeting.getAgendaItems()) {
                    if (item.getPolls() != null) {
                        for (Poll poll : item.getPolls()) {
                            poll.setDataSource(DataSourceType.DM_API);
                            log.debug(Constants.MarkerSync, ">> poll: {}", poll);
                            if (poll.getExtAgendaItemId() == null || poll.getExtPollRouteId() == null) {
                                log.warn(Constants.MarkerSync, "Skipping poll details - missing ext IDs for poll '{}'", poll.getName());
                                continue;
                            }
                            try {
                                dataImport.loadPollDetails(poll, membersMap);
                            } catch (Exception e) {
                                log.warn(Constants.MarkerSync, "Could not load poll details (votes) for poll '{}' - saving poll with vote counts only: {}",
                                        poll.getName(), e.getMessage());
                            }
                        }
                    }
                }
            } else {
                // PRIORITY 2: PDF fallback
                String pdfUrl = findVotingPdfUrl(meeting);
                log.info(Constants.MarkerSync, "PDF fallback for '{}': pdfUrl={}", meeting.getName(), pdfUrl);
                if (pdfUrl != null) {
                    log.info(Constants.MarkerSync, "Loading votes from PDF for meeting: {}", meeting.getName());
                    new DMPdfImporter().importVotesFromPdf(meeting, pdfUrl, membersMap);
                } else {
                    log.info(Constants.MarkerSync, "No voting PDF found for meeting: {} (attachments: {})",
                            meeting.getName(), meeting.getAttachments() != null ?
                                    meeting.getAttachments().stream().map(MeetingAttachment::getName)
                                            .collect(Collectors.joining(", ")) : "null");
                }
            }

            createMissingMembersFromVotes(meeting, membersMap);
        }
        log.debug(Constants.MarkerSync, "NEW MEETING: {}", meeting);
    }

    private String findVotingPdfUrl(Meeting meeting) {
        if (meeting.getAttachments() == null) return null;
        return meeting.getAttachments().stream()
                .filter(a -> a.getSource() != null && a.getName() != null
                        && a.getName().toLowerCase().contains("hlasovan"))
                .map(MeetingAttachment::getSource)
                .findFirst().orElse(null);
    }

    /**
     * Auto-create Politician + CouncilMember records for DM API voters that couldn't be matched
     * to existing council members. This handles old-season members not in the crawler data.
     */
    private void createMissingMembersFromVotes(Meeting meeting, Map<String, CouncilMember> membersMap) {
        int created = 0;
        for (AgendaItem item : meeting.getAgendaItems()) {
            if (item.getPolls() == null) continue;
            for (Poll poll : item.getPolls()) {
                if (poll.getVotes() == null) continue;
                for (Vote vote : poll.getVotes()) {
                    if (vote.getCouncilMember() != null || vote.getVoterName() == null) continue;

                    String voterName = vote.getVoterName();
                    String nameKey = toSimpleNameWithoutAccents(voterName);

                    // Check membersMap (may have been created from earlier poll in same meeting)
                    CouncilMember member = membersMap.get(nameKey);
                    if (member == null) {
                        String[] parts = nameKey.split("\\s", 2);
                        if (parts.length == 2) {
                            member = membersMap.get(parts[1] + " " + parts[0]);
                        }
                    }
                    if (member != null) {
                        vote.setCouncilMember(member);
                        continue;
                    }

                    // Find or create politician
                    Politician politician = findPoliticianByName(nameKey);
                    if (politician == null) {
                        String simpleName = PollsUtils.toSimpleName(voterName);
                        String titles = PollsUtils.getTitles(voterName);
                        politician = new Politician();
                        politician.setName(simpleName);
                        politician.setRef(PollsUtils.generateUniqueKeyReference());
                        politician.setTitles(titles);
                        politician = politicianRepository.save(politician);
                        if (politiciansMap != null) {
                            politiciansMap.put(nameKey, politician);
                        }
                        log.info(Constants.MarkerSync, "Auto-created politician from DM voter: '{}'", simpleName);
                    }

                    // Create council member
                    CouncilMember newMember = new CouncilMember();
                    newMember.setRef(PollsUtils.generateUniqueKeyReference());
                    newMember.setPolitician(politician);
                    newMember.setTown(meeting.getTown());
                    newMember.setSeason(meeting.getSeason());
                    newMember.setInstitution(meeting.getInstitution());
                    newMember = councilMemberRepository.save(newMember);

                    // Update membersMap for subsequent polls
                    membersMap.put(nameKey, newMember);
                    String[] parts = nameKey.split("\\s", 2);
                    if (parts.length == 2) {
                        membersMap.put(parts[1] + " " + parts[0], newMember);
                    }

                    vote.setCouncilMember(newMember);
                    created++;
                }
            }
        }
        if (created > 0) {
            log.info(Constants.MarkerSync, "Auto-created {} council member(s) for meeting '{}' (season: {})",
                    created, meeting.getName(), meeting.getSeason().getRef());
        }
    }

    private Politician findPoliticianByName(String nameKey) {
        if (politiciansMap == null) return null;
        Politician politician = politiciansMap.get(nameKey);
        if (politician == null) {
            String[] parts = nameKey.split("\\s", 2);
            if (parts.length == 2) {
                politician = politiciansMap.get(parts[1] + " " + parts[0]);
            }
        }
        if (politician != null && politician.getId() != 0) {
            // Re-attach detached entity within current transaction
            return politicianRepository.findById(politician.getId()).orElse(politician);
        }
        return politician;
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
        log.debug(Constants.MarkerSync, "Get membersMap for {}:{}:", town.getName(), institution.getName());
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
        log.debug(Constants.MarkerSync, " -- loadCouncilMembers for season: {}", season);
        Map<String, CouncilMember> membersMap = new HashMap<>();
        Set<CouncilMember> members = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), institution.getType());
        log.debug(Constants.MarkerSync, " -- members: {}", (members != null ? members.size() : 0));
        if (members == null || members.isEmpty()) { // TODO: preco vracia empty set?
            self.syncCouncilMembers(town);
            members = councilMemberRepository.getByTownAndSeasonAndInstitution(town.getRef(), season.getRef(), institution.getType());
            log.debug(Constants.MarkerSync, " -- members: {}", (members != null ? members.size() : 0));
        }
        if (members == null || members.isEmpty()) {
            log.warn(Constants.MarkerSync, "No CouncilMembers found for town {}, season {}, institution {} - votes will be saved without member links",
                    town.getRef(), season.getRef(), institution.getType());
        }
        for (CouncilMember councilMember : members) {
            log.debug(Constants.MarkerSync, "Loaded Council Member > {}", councilMember.getPolitician().getName());
            String nameKey = PollsUtils.toSimpleNameWithoutAccents(councilMember.getPolitician().getName());
            membersMap.put(nameKey, councilMember);
            // Also store under reversed name order for matching DM API "Lastname Firstname" format
            String[] parts = nameKey.split("\\s", 2);
            if (parts.length == 2) {
                membersMap.put(parts[1] + " " + parts[0], councilMember);
            }
        }
        String membersKey = PollsUtils.generateMemberKey(town, season, institution.getType());
        log.debug(Constants.MarkerSync, "Loaded Council Member Group > {}", membersKey);
        allMembersMap.put(membersKey, membersMap);
    }

}
