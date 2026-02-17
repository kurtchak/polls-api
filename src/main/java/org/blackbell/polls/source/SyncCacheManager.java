package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.*;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static org.blackbell.polls.common.PollsUtils.toSimpleNameWithoutAccents;

/**
 * Manages shared caches used during synchronization (towns, seasons, parties, politicians, institutions).
 */
@Component
public class SyncCacheManager {
    private static final Logger log = LoggerFactory.getLogger(SyncCacheManager.class);

    private final TownRepository townRepository;
    private final SeasonRepository seasonRepository;
    private final PartyRepository partyRepository;
    private final PoliticianRepository politicianRepository;
    private final InstitutionRepository institutionRepository;

    private Map<String, Town> townsMap;
    private Map<String, Season> seasonsMap;
    private Map<String, Party> partiesMap;
    private Map<String, Politician> politiciansMap;
    private Map<InstitutionType, List<Institution>> institutionsMap;

    public SyncCacheManager(TownRepository townRepository, SeasonRepository seasonRepository,
                            PartyRepository partyRepository, PoliticianRepository politicianRepository,
                            InstitutionRepository institutionRepository) {
        this.townRepository = townRepository;
        this.seasonRepository = seasonRepository;
        this.partyRepository = partyRepository;
        this.politicianRepository = politicianRepository;
        this.institutionRepository = institutionRepository;
    }

    public Town getTown(String ref) {
        if (townsMap == null) {
            loadTownsMap();
        }
        return townsMap.get(ref);
    }

    public Set<String> getTownsRefs() {
        if (townsMap == null) {
            loadTownsMap();
        }
        return townsMap.keySet();
    }

    public Season getSeason(String ref) {
        if (seasonsMap == null) {
            loadSeasonsMap();
        }
        return seasonsMap.get(ref);
    }

    public Set<String> getSeasonsRefs() {
        if (seasonsMap == null) {
            loadSeasonsMap();
        }
        return seasonsMap.keySet();
    }

    public Map<String, Party> getPartiesMap() {
        if (partiesMap == null) {
            loadPartiesMap();
        }
        return partiesMap;
    }

    public Map<String, Politician> getPoliticiansMap() {
        return politiciansMap;
    }

    public void loadPoliticiansMap(Town town) {
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

    public void putPolitician(String key, Politician politician) {
        if (politiciansMap != null) {
            politiciansMap.put(key, politician);
        }
    }

    public Map<InstitutionType, List<Institution>> loadInstitutionsMap() {
        List<Institution> institutions = institutionRepository.findAll();
        Map<InstitutionType, List<Institution>> map = new HashMap<>();
        for (Institution institution : institutions) {
            map.computeIfAbsent(institution.getType(), k -> new ArrayList<>())
                    .add(institution);
        }
        this.institutionsMap = map;
        return map;
    }

    public Map<InstitutionType, List<Institution>> getInstitutionsMap() {
        return institutionsMap;
    }

    /**
     * Reset all caches — called before sync to ensure fresh data.
     */
    public void resetCaches() {
        townsMap = null;
        seasonsMap = null;
        partiesMap = null;
        politiciansMap = null;
        institutionsMap = null;
    }

    public void resetSeasonsMap() {
        seasonsMap = null;
    }

    public void resetTownsMap() {
        townsMap = null;
    }

    private void loadTownsMap() {
        townsMap = townRepository.findAll().stream()
                .collect(Collectors.toMap(Town::getRef, t -> t));
    }

    private void loadSeasonsMap() {
        seasonsMap = seasonRepository.findAll().stream()
                .collect(Collectors.toMap(Season::getRef, p -> p));
    }

    private void loadPartiesMap() {
        partiesMap = partyRepository.findAll().stream()
                .collect(Collectors.toMap(Party::getName, p -> p));
    }
}
