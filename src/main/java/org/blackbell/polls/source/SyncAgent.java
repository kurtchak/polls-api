package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.DataSourceType;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.*;
import org.blackbell.polls.source.dm.DMPdfImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.blackbell.polls.sync.SyncProgress;

import java.util.*;
import java.util.stream.Collectors;

import static org.blackbell.polls.common.PollsUtils.toSimpleNameWithoutAccents;

/**
 * Created by kurtcha on 25.2.2018.
 */
@Component
public class SyncAgent {
    private static final Logger log = LoggerFactory.getLogger(SyncAgent.class);

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
    private final DataSourceResolver resolver;

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
                     @Lazy SyncAgent self, SyncProgress syncProgress,
                     DataSourceResolver resolver) {
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
        this.resolver = resolver;
    }

    // --- Resolver helper methods ---

    @FunctionalInterface
    private interface CheckedFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * FIRST WINS: skúsi zdroje v poradí, vráti prvý úspešný výsledok.
     */
    private <T> T resolveAndLoad(Town town, String seasonRef,
            InstitutionType inst, DataOperation op,
            CheckedFunction<DataImport, T> loader) {
        List<DataImport> imports = resolver.resolve(town, seasonRef, inst, op);
        for (DataImport di : imports) {
            try {
                T result = loader.apply(di);
                if (result != null && !(result instanceof Collection<?> c && c.isEmpty())) {
                    log.info("Source {} provided data for {}/{}/{}",
                        di.getClass().getSimpleName(), town.getRef(), seasonRef, op);
                    return result;
                }
            } catch (Exception e) {
                log.warn("Source {} failed for {}/{}/{}: {}",
                    di.getClass().getSimpleName(), town.getRef(), seasonRef, op, e.getMessage());
            }
        }
        log.error("No data source provided data for {}/{}/{}", town.getRef(), seasonRef, op);
        return null;
    }

    /**
     * AGGREGATE: zozbiera výsledky zo všetkých zdrojov (pre sezóny).
     */
    private <T> List<T> resolveAndAggregate(Town town, DataOperation op,
            CheckedFunction<DataImport, List<T>> loader) {
        List<T> aggregated = new ArrayList<>();
        for (DataImport di : resolver.allForTown(town)) {
            try {
                List<T> result = loader.apply(di);
                if (result != null) aggregated.addAll(result);
            } catch (Exception e) {
                log.warn("Source {} failed for {}/{}: {}",
                    di.getClass().getSimpleName(), town.getRef(), op, e.getMessage());
            }
        }
        if (aggregated.isEmpty()) {
            log.error("No source provided {} for town {}", op, town.getRef());
        }
        return aggregated;
    }

    // --- Council Members ---

    @Transactional
    public void syncCouncilMembers(Town town) {
        log.info(Constants.MarkerSync, "syncCouncilMembers...");
        Institution townCouncil = institutionRepository.findByType(InstitutionType.ZASTUPITELSTVO);

        // Load existing politicians for reuse across seasons (tracking "prezliekači")
        loadPoliticiansMap(town);

        for (String seasonRef : getSeasonsRefs()) {
            Set<CouncilMember> existingMembers = councilMemberRepository
                    .getByTownAndSeasonAndInstitution(
                            town.getRef(),
                            seasonRef,
                            InstitutionType.ZASTUPITELSTVO);

            log.info("COUNCIL MEMBERS for {} season {}: {}", town.getRef(), seasonRef, existingMembers.size());

            if (!existingMembers.isEmpty()) continue;

            List<CouncilMember> newMembers = resolveAndLoad(town, seasonRef,
                    InstitutionType.ZASTUPITELSTVO, DataOperation.MEMBERS,
                    di -> di.loadMembers(town, getSeason(seasonRef), townCouncil));

            if (newMembers != null && !newMembers.isEmpty()) {
                reuseExistingPoliticians(newMembers, town, townCouncil);
                councilMemberRepository.saveAll(newMembers);
                log.info("Saved {} council members for {} season {}", newMembers.size(), town.getRef(), seasonRef);
            }
        }
        log.info(Constants.MarkerSync, "Council Members Sync finished");
    }

    /**
     * Reuse existing politicians across seasons and set town/institution on members.
     */
    private void reuseExistingPoliticians(List<CouncilMember> members, Town town, Institution institution) {
        for (CouncilMember cm : members) {
            // Ensure town and institution are set
            if (cm.getTown() == null) cm.setTown(town);
            if (cm.getInstitution() == null) cm.setInstitution(institution);

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
        }
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
        townsMap = townRepository.findAll().stream()
                .collect(Collectors.toMap(Town::getRef, t -> t));
    }

    @Scheduled(fixedRateString = "${sync.fixed-rate-ms}", initialDelayString = "${sync.initial-delay-ms}")
    public synchronized void sync() {
        syncAll();
    }

    /**
     * Manually trigger sync for all towns. Runs async when called via self-proxy.
     * Returns false if sync is already running.
     */
    @Async
    public synchronized void triggerSync(String townRef) {
        if (townRef != null) {
            // Reload towns map to pick up newly added towns
            townsMap = null;
            Town town = getTown(townRef);
            if (town == null) {
                log.warn(Constants.MarkerSync, "Town not found: {}", townRef);
                return;
            }
            syncSingleTown(town);
        } else {
            syncAll();
        }
    }

    public boolean isRunning() {
        return syncProgress.getStatus().isRunning();
    }

    private synchronized void syncAll() {
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
                syncTown(town);
            });
        } finally {
            syncProgress.finishSync();
            log.info(Constants.MarkerSync, "Synchronization finished");
        }
    }

    private void syncSingleTown(Town town) {
        institutionsMap = loadInstitutionsMap(institutionRepository.findAll());
        log.info(Constants.MarkerSync, "Manual sync started for town: {}", town.getRef());
        syncProgress.startSync();

        try {
            // Reload seasons map to pick up any new seasons
            seasonsMap = null;
            syncTown(town);
        } finally {
            syncProgress.finishSync();
            log.info(Constants.MarkerSync, "Manual sync finished for town: {}", town.getRef());
        }
    }

    private void syncTown(Town town) {
        syncProgress.startTown(town.getRef());
        syncSeasons(town);
        self.syncCouncilMembers(town);

        getSeasonsRefs().forEach(seasonRef -> syncSeasonMeetings(town, getSeason(seasonRef)));
        self.saveTownLastSyncDate(town);
        log.info(Constants.MarkerSync, "Synchronization finished for town: {}", town.getRef());
    }

    private void syncSeasons(Town town) {
        try {
            List<Season> retrievedSeasons = resolveAndAggregate(town, DataOperation.SEASONS,
                    di -> di.loadSeasons(town));
            log.info(Constants.MarkerSync, "RETRIEVED SEASONS: {}", retrievedSeasons);

            // Load Former Seasons
            List<Season> formerSeasons = seasonRepository.findAll();
            log.info(Constants.MarkerSync, "FORMER SEASONS: {}", formerSeasons);

            // Save New Seasons
            retrievedSeasons.stream().filter(season -> !formerSeasons.contains(season)).forEach(self::saveNewSeason);
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {}s seasons synchronization.", town.getName(), e);
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

    private void syncSeasonMeetings(Town town, Season season) {
        // Skip historical seasons where all meetings are already complete
        if (isHistoricalSeason(season) && isSeasonFullySynced(town, season)) {
            log.info(Constants.MarkerSync, "{}: Skipping fully synced historical season {}", town.getName(), season.getRef());
            return;
        }
        log.info(Constants.MarkerSync, "{}: syncSeasonMeetings {}", town.getName(), season.getRef());

        institutionsMap.keySet()
                .forEach(type -> self.syncSeasonMeetings(town, season, institutionsMap.get(type).get(0)));
    }

    /**
     * A season is historical if its end year is before the current year.
     * Season ref format: "2014-2018" → end year = 2018.
     */
    private boolean isHistoricalSeason(Season season) {
        try {
            String ref = season.getRef();
            int endYear = Integer.parseInt(ref.substring(ref.indexOf('-') + 1));
            return endYear < java.time.Year.now().getValue();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * A season is fully synced if it has meetings and ALL of them are syncComplete.
     */
    private boolean isSeasonFullySynced(Town town, Season season) {
        long total = meetingRepository.countMeetingsByTownAndSeason(town.getRef(), season.getRef());
        if (total == 0) return false; // Never synced
        long incomplete = meetingRepository.countIncompleteMeetings(town.getRef(), season.getRef());
        return incomplete == 0;
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
            return;
        }
        log.info(Constants.MarkerSync, "{}:{}:{}: syncSeasonMeetings", town.getName(), season.getName(), institution.getName());
        try {
            List<Meeting> meetings = resolveAndLoad(town, season.getRef(),
                    institution.getType(), DataOperation.MEETINGS,
                    di -> di.loadMeetings(town, season, institution));
            if (meetings != null) {
                // Resolve the DataImport for meeting details (same source that provided meetings)
                DataImport detailsImport = resolveDetailImport(town, season, institution);
                syncProgress.startSeason(town.getRef(), season.getRef(), meetings.size());
                for (Meeting meeting : meetings) {
                    try {
                        self.syncSingleMeeting(meeting, detailsImport);
                    } catch (Exception e) {
                        log.error(Constants.MarkerSync, "Failed to sync meeting '{}': {}", meeting.getName(), e.getMessage(), e);
                    } finally {
                        syncProgress.meetingProcessed();
                    }
                }
            } else {
                Season s = getSeason(season.getRef());
                if (s != null) {
                    s.setSyncError("No data source available for meetings");
                    seasonRepository.save(s);
                }
            }
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {} meetings synchronization.", season.getRef(), e);
        }
    }

    /**
     * Resolve the DataImport to use for meeting details and poll details.
     * Uses FIRST WINS strategy from the resolver.
     */
    private DataImport resolveDetailImport(Town town, Season season, Institution institution) {
        List<DataImport> imports = resolver.resolve(town, season.getRef(),
                institution.getType(), DataOperation.MEETING_DETAILS);
        return imports.isEmpty() ? null : imports.get(0);
    }

    @Transactional
    public void syncSingleMeeting(Meeting meeting, DataImport dataImport) {
        Meeting existing = meetingRepository.findByExtId(meeting.getExtId());
        if (existing != null) {
            // Fast skip: already fully synced
            if (existing.isSyncComplete()) {
                log.debug(Constants.MarkerSync, "Skipping complete meeting: {}", meeting.getName());
                return;
            }
            if (existing.getSyncError() != null) {
                log.info(Constants.MarkerSync, "Retrying previously failed meeting: {} (error: {})",
                        meeting.getName(), existing.getSyncError());
            } else if (existing.isComplete()) {
                // Mark as complete for future fast skip
                existing.setSyncComplete(true);
                meetingRepository.save(existing);
                log.debug(Constants.MarkerSync, "Marked meeting as syncComplete: {}", meeting.getName());
                return;
            } else if (existing.hasVotes() && existing.hasUnmatchedVotes()) {
                log.info(Constants.MarkerSync, "Re-loading meeting with unmatched votes: {}", meeting.getName());
            } else if (!existing.hasVotes() && existing.hasPolls()) {
                log.info(Constants.MarkerSync, "Re-loading meeting without individual votes: {}", meeting.getName());
            } else {
                log.info(Constants.MarkerSync, "Re-loading incomplete meeting (0 agenda items): {}", meeting.getName());
            }
            meetingRepository.delete(existing);
            meetingRepository.flush();
        }
        try {
            loadMeeting(meeting, dataImport);
            meeting.setSyncError(null);
            // Check if newly loaded meeting is complete
            meeting.setSyncComplete(meeting.isComplete());
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "Error loading meeting '{}': {}", meeting.getName(), e.getMessage(), e);
            meeting.setSyncError(e.getMessage());
            meeting.setSyncComplete(false);
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
                for (AgendaItem item : meeting.getAgendaItems()) {
                    if (item.getPolls() != null) {
                        for (Poll poll : item.getPolls()) {
                            if (poll.getDataSource() == null) {
                                poll.setDataSource(DataSourceType.DM_API);
                            }
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
                // PDF fallback (runtime decision, stays in SyncAgent)
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
        if (members == null || members.isEmpty()) {
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
