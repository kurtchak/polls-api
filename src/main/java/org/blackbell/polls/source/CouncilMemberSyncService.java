package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.common.PollsUtils;
import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.CouncilMemberRepository;
import org.blackbell.polls.domain.repositories.InstitutionRepository;
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
    private final SyncCacheManager cacheManager;
    private final PoliticianMatchingService politicianMatchingService;

    private Map<String, Map<String, CouncilMember>> allMembersMap;

    public CouncilMemberSyncService(DataSourceResolver resolver,
                                    CouncilMemberRepository councilMemberRepository,
                                    InstitutionRepository institutionRepository,
                                    SyncCacheManager cacheManager,
                                    PoliticianMatchingService politicianMatchingService) {
        this.resolver = resolver;
        this.councilMemberRepository = councilMemberRepository;
        this.institutionRepository = institutionRepository;
        this.cacheManager = cacheManager;
        this.politicianMatchingService = politicianMatchingService;
    }

    @Transactional
    public void syncCouncilMembers(Town town) {
        log.info(Constants.MarkerSync, "syncCouncilMembers...");
        Institution townCouncil = institutionRepository.findByType(InstitutionType.ZASTUPITELSTVO);

        cacheManager.loadPoliticiansMap(town);

        for (String seasonRef : cacheManager.getSeasonsRefs()) {
            Set<CouncilMember> existingMembers = councilMemberRepository
                    .getByTownAndSeasonAndInstitution(
                            town.getRef(),
                            seasonRef,
                            InstitutionType.ZASTUPITELSTVO);

            log.info("COUNCIL MEMBERS for {} season {}: {}", town.getRef(), seasonRef, existingMembers.size());

            if (!existingMembers.isEmpty()) continue;

            List<CouncilMember> newMembers = resolver.resolveAndLoad(town, seasonRef,
                    InstitutionType.ZASTUPITELSTVO, DataOperation.MEMBERS,
                    di -> di.loadMembers(town, cacheManager.getSeason(seasonRef), townCouncil));

            if (newMembers != null && !newMembers.isEmpty()) {
                politicianMatchingService.reuseExistingPoliticians(newMembers, town, townCouncil);
                councilMemberRepository.saveAll(newMembers);
                log.info("Saved {} council members for {} season {}", newMembers.size(), town.getRef(), seasonRef);
            }
        }
        log.info(Constants.MarkerSync, "Council Members Sync finished");
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
