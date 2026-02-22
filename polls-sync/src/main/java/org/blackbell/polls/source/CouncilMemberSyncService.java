package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.DataOperation;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.model.enums.Source;
import java.util.Set;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.relate.ClubParty;
import org.blackbell.polls.domain.model.relate.PartyNominee;
import org.blackbell.polls.domain.repositories.CouncilMemberRepository;
import org.blackbell.polls.domain.repositories.InstitutionRepository;
import org.blackbell.polls.domain.repositories.PartyRepository;
import org.blackbell.polls.sync.SyncEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Handles synchronization of council members from data sources.
 */
@Component
public class CouncilMemberSyncService {
    private static final Logger log = LoggerFactory.getLogger(CouncilMemberSyncService.class);

    private final DataSourceResolver resolver;
    private final CouncilMemberRepository councilMemberRepository;
    private final InstitutionRepository institutionRepository;
    private final PartyRepository partyRepository;
    private final SyncCacheManager cacheManager;
    private final PoliticianMatchingService politicianMatchingService;
    private final SyncEventBroadcaster eventBroadcaster;

    private Map<String, Map<String, CouncilMember>> allMembersMap;

    public CouncilMemberSyncService(DataSourceResolver resolver,
                                    CouncilMemberRepository councilMemberRepository,
                                    InstitutionRepository institutionRepository,
                                    PartyRepository partyRepository,
                                    SyncCacheManager cacheManager,
                                    PoliticianMatchingService politicianMatchingService,
                                    SyncEventBroadcaster eventBroadcaster) {
        this.resolver = resolver;
        this.councilMemberRepository = councilMemberRepository;
        this.institutionRepository = institutionRepository;
        this.partyRepository = partyRepository;
        this.cacheManager = cacheManager;
        this.politicianMatchingService = politicianMatchingService;
        this.eventBroadcaster = eventBroadcaster;
    }

    @Transactional
    public void syncCouncilMembers(Town town, Set<Season> seasons) {
        log.info(Constants.MarkerSync, "syncCouncilMembers...");
        Institution townCouncil = institutionRepository.findByType(InstitutionType.ZASTUPITELSTVO);

        cacheManager.loadPoliticiansMap(town);

        for (Season seasonObj : seasons) {
            String seasonRef = seasonObj.getRef();
            // Fix orphan members saved without town/institution (updatable=false bypassed via native SQL)
            Season season = cacheManager.getSeason(seasonRef);
            if (season != null && townCouncil != null) {
                int fixed = councilMemberRepository.fixOrphanMembers(
                        town.getId(), season.getId(), townCouncil.getId());
                if (fixed > 0) {
                    log.info("Fixed {} orphan members (null town) for {} season {}",
                            fixed, town.getRef(), seasonRef);
                    councilMemberRepository.flush();
                }
            }

            Set<CouncilMember> existingMembers = councilMemberRepository
                    .getByTownAndSeasonAndInstitution(
                            town.getRef(),
                            seasonRef,
                            InstitutionType.ZASTUPITELSTVO);

            log.info("COUNCIL MEMBERS for {} season {}: {}", town.getRef(), seasonRef, existingMembers.size());

            if (!existingMembers.isEmpty()) {
                if (membersNeedEnrichment(existingMembers)) {
                    enrichExistingMembers(existingMembers, town, seasonRef, townCouncil);
                }
                continue;
            }

            DataSourceResolver.SourcedResult<List<CouncilMember>> result = resolver.resolveAndLoad(
                    town, seasonRef, InstitutionType.ZASTUPITELSTVO, DataOperation.MEMBERS,
                    di -> di.loadMembers(town, cacheManager.getSeason(seasonRef), townCouncil));

            if (result != null && result.data() != null && !result.data().isEmpty()) {
                List<CouncilMember> newMembers = result.data();
                Source membersSource = result.source();
                for (CouncilMember member : newMembers) {
                    member.setDataSource(membersSource);
                }
                // Resolve Party references BEFORE reuseExistingPoliticians —
                // that method adds PartyNominees to managed politicians, so Party
                // entities must already be managed to avoid duplicate key on auto-flush
                Map<String, Party> existingParties = new HashMap<>();
                partyRepository.findAll().forEach(p -> existingParties.put(p.getRef(), p));
                for (CouncilMember member : newMembers) {
                    resolvePartyReferences(member, existingParties);
                }

                politicianMatchingService.reuseExistingPoliticians(newMembers, town, townCouncil);
                councilMemberRepository.saveAll(newMembers);
                log.info("Saved {} council members for {} season {} (source: {})",
                        newMembers.size(), town.getRef(), seasonRef, membersSource);
                eventBroadcaster.emit("SUCCESS", town.getRef(), seasonRef, "members",
                        "Loaded " + newMembers.size() + " council members");
            }
        }
        log.info(Constants.MarkerSync, "Council Members Sync finished");
    }

    /**
     * Check if existing members need enrichment (e.g. loaded with basic info only, missing photo/email/club).
     * Uses noneMatch: triggers only when zero members have a given field,
     * meaning initial enrichment hasn't been done yet. Members that legitimately
     * lack a club (e.g. primátor) won't cause repeated re-enrichment.
     */
    private boolean membersNeedEnrichment(Set<CouncilMember> members) {
        boolean noPhotos = members.stream()
                .noneMatch(m -> m.getPolitician().getPicture() != null);
        boolean noEmails = members.stream()
                .noneMatch(m -> m.getPolitician().getEmail() != null);
        boolean noClubs = members.stream()
                .noneMatch(m -> m.getClubMembers() != null && !m.getClubMembers().isEmpty());
        return noPhotos || noEmails || noClubs;
    }

    /**
     * Re-scrape fresh member data and merge detail info into existing managed entities.
     * Enriches: email, phone, description, club membership, party nominations.
     */
    private void enrichExistingMembers(Set<CouncilMember> existingMembers, Town town,
                                       String seasonRef, Institution townCouncil) {
        log.info("Enriching {} members for {} season {} (no emails found)",
                existingMembers.size(), town.getRef(), seasonRef);

        DataSourceResolver.SourcedResult<List<CouncilMember>> freshResult = resolver.resolveAndLoad(
                town, seasonRef, InstitutionType.ZASTUPITELSTVO, DataOperation.MEMBERS,
                di -> di.loadMembers(town, cacheManager.getSeason(seasonRef), townCouncil));

        if (freshResult == null || freshResult.data() == null || freshResult.data().isEmpty()) return;
        List<CouncilMember> freshMembers = freshResult.data();

        // Check if fresh data actually contains detail info (photo/email/phone/club).
        // Data sources like DM API return basic member info only — no point enriching.
        boolean hasDetailData = freshMembers.stream().anyMatch(m ->
                m.getPolitician().getPicture() != null
                || m.getPolitician().getEmail() != null
                || m.getPolitician().getPhone() != null
                || (m.getClubMembers() != null && !m.getClubMembers().isEmpty()));
        if (!hasDetailData) {
            log.info("Fresh data has no detail info for {} season {} — skipping enrichment",
                    town.getRef(), seasonRef);
            return;
        }

        // Load existing parties to avoid duplicate key violations on Party.ref (unique)
        Map<String, Party> existingParties = new HashMap<>();
        partyRepository.findAll().forEach(p -> existingParties.put(p.getRef(), p));

        // Replace transient Party refs with managed entities in all fresh members
        for (CouncilMember fresh : freshMembers) {
            resolvePartyReferences(fresh, existingParties);
        }

        // Build lookup by normalized name + reversed name order
        Map<String, CouncilMember> freshMap = new HashMap<>();
        for (CouncilMember fresh : freshMembers) {
            String key = PollsUtils.toSimpleNameWithoutAccents(fresh.getPolitician().getName());
            freshMap.put(key, fresh);
            String[] parts = key.split("\\s", 2);
            if (parts.length == 2) {
                freshMap.put(parts[1] + " " + parts[0], fresh);
            }
        }

        int enriched = 0;
        for (CouncilMember existing : existingMembers) {
            String key = PollsUtils.toSimpleNameWithoutAccents(existing.getPolitician().getName());
            CouncilMember fresh = freshMap.get(key);
            if (fresh == null) {
                log.warn("No fresh match for existing member: {} (key: {})", PollsUtils.deAccent(existing.getPolitician().getName()), key);
                continue;
            }

            if (enrichMember(existing, fresh)) {
                enriched++;
            }
        }

        log.info("Enriched {}/{} members for {} season {}",
                enriched, existingMembers.size(), town.getRef(), seasonRef);
        if (enriched > 0) {
            eventBroadcaster.emit("INFO", town.getRef(), seasonRef, "members",
                    "Enriched " + enriched + " members with details");
        }
    }

    /**
     * Merge detail fields from a freshly scraped member into an existing managed entity.
     * Only fills in fields that are missing on the existing member.
     */
    private boolean enrichMember(CouncilMember existing, CouncilMember fresh) {
        Politician existingPol = existing.getPolitician();
        Politician freshPol = fresh.getPolitician();
        boolean updated = false;

        if (existingPol.getPicture() == null && freshPol.getPicture() != null) {
            existingPol.setPicture(freshPol.getPicture());
            updated = true;
        }
        if (existingPol.getEmail() == null && freshPol.getEmail() != null) {
            existingPol.setEmail(freshPol.getEmail());
            updated = true;
        }
        if (existingPol.getPhone() == null && freshPol.getPhone() != null) {
            existingPol.setPhone(freshPol.getPhone());
            updated = true;
        }

        // Overwrite party text with district info from detail page
        if (fresh.getDescription() != null && fresh.getDescription().startsWith("Volebný obvod")) {
            existing.setDescription(fresh.getDescription());
            updated = true;
        }

        // Add club membership if missing
        if ((existing.getClubMembers() == null || existing.getClubMembers().isEmpty())
                && fresh.getClubMembers() != null && !fresh.getClubMembers().isEmpty()) {
            for (ClubMember cm : fresh.getClubMembers()) {
                cm.setCouncilMember(existing);
                existing.addClubMember(cm);
            }
            updated = true;
        }

        // Add party nominations if missing
        if ((existingPol.getPartyNominees() == null || existingPol.getPartyNominees().isEmpty())
                && freshPol.getPartyNominees() != null && !freshPol.getPartyNominees().isEmpty()) {
            for (PartyNominee pn : freshPol.getPartyNominees()) {
                existingPol.addPartyNominee(pn);
            }
            updated = true;
        }

        if (updated) {
            log.info("Enriched member: {} (email: {}, phone: {}, club: {})",
                    PollsUtils.deAccent(existingPol.getName()), existingPol.getEmail(),
                    existingPol.getPhone(),
                    existing.getClubMember() != null ? existing.getClubMember().getClub().getName() : "none");
        } else {
            log.info("No new data for member: {} (fresh email: {}, fresh phone: {})",
                    PollsUtils.deAccent(existingPol.getName()), freshPol.getEmail(), freshPol.getPhone());
        }

        return updated;
    }

    /**
     * Replace transient Party references in a freshly scraped member with managed entities from DB.
     * Prevents duplicate key violations on Party.ref (unique) during cascade persist.
     */
    private void resolvePartyReferences(CouncilMember fresh, Map<String, Party> existingParties) {
        // Fix parties in PartyNominees
        Politician pol = fresh.getPolitician();
        if (pol.getPartyNominees() != null) {
            for (PartyNominee pn : pol.getPartyNominees()) {
                if (pn.getParty() != null) {
                    Party managed = existingParties.get(pn.getParty().getRef());
                    if (managed != null) {
                        pn.setParty(managed);
                    }
                }
            }
        }

        // Fix parties in ClubParties (Club → ClubParty → Party cascade)
        if (fresh.getClubMembers() != null) {
            for (ClubMember cm : fresh.getClubMembers()) {
                if (cm.getClub() != null && cm.getClub().getClubParties() != null) {
                    for (ClubParty cp : cm.getClub().getClubParties()) {
                        if (cp.getParty() != null) {
                            Party managed = existingParties.get(cp.getParty().getRef());
                            if (managed != null) {
                                cp.setParty(managed);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get or load the members map for a given town/season/institution.
     * The orchestrator guarantees syncCouncilMembers has been called before this.
     */
    public Map<String, CouncilMember> getMembersMap(Town town, Season season, Institution institution) {
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

    /**
     * Reset members cache between sync runs.
     */
    public void resetMembersMap() {
        allMembersMap = null;
    }

    private void loadCouncilMembers(Town town, Season season, Institution institution) {
        log.debug(Constants.MarkerSync, " -- loadCouncilMembers for season: {}", season);
        Map<String, CouncilMember> membersMap = new HashMap<>();
        Set<CouncilMember> members = councilMemberRepository.getByTownAndSeasonAndInstitution(
                town.getRef(), season.getRef(), institution.getType());
        log.debug(Constants.MarkerSync, " -- members: {}", (members != null ? members.size() : 0));
        if (members == null || members.isEmpty()) {
            log.warn(Constants.MarkerSync, "No CouncilMembers found for town {}, season {}, institution {} - votes will be saved without member links",
                    town.getRef(), season.getRef(), institution.getType());
        }
        if (members != null) {
            for (CouncilMember councilMember : members) {
                log.debug(Constants.MarkerSync, "Loaded Council Member > {}", councilMember.getPolitician().getName());
                String nameKey = PollsUtils.toSimpleNameWithoutAccents(councilMember.getPolitician().getName());
                membersMap.put(nameKey, councilMember);
                String[] parts = nameKey.split("\\s", 2);
                if (parts.length == 2) {
                    membersMap.put(parts[1] + " " + parts[0], councilMember);
                }
            }
        }
        String membersKey = PollsUtils.generateMemberKey(town, season, institution.getType());
        log.debug(Constants.MarkerSync, "Loaded Council Member Group > {}", membersKey);
        allMembersMap.put(membersKey, membersMap);
    }
}
