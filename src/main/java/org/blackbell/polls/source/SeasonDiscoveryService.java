package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.Institution;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SeasonDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SeasonDiscoveryService.class);

    private final SeasonRepository seasonRepository;
    private final TownRepository townRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingSyncService meetingSyncService;
    private final CouncilMemberSyncService councilMemberSyncService;
    private final SyncCacheManager cacheManager;
    private final TransactionTemplate txTemplate;

    public SeasonDiscoveryService(SeasonRepository seasonRepository,
                                  TownRepository townRepository,
                                  MeetingRepository meetingRepository,
                                  MeetingSyncService meetingSyncService,
                                  CouncilMemberSyncService councilMemberSyncService,
                                  SyncCacheManager cacheManager,
                                  PlatformTransactionManager txManager) {
        this.seasonRepository = seasonRepository;
        this.townRepository = townRepository;
        this.meetingRepository = meetingRepository;
        this.meetingSyncService = meetingSyncService;
        this.councilMemberSyncService = councilMemberSyncService;
        this.cacheManager = cacheManager;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    public record DiscoveryResult(String status, String seasonRef, long meetingCount, String message) {
        static DiscoveryResult noSeasons() {
            return new DiscoveryResult("error", null, 0, "Žiadne sezóny pre toto mesto");
        }

        static DiscoveryResult cannotCalculate(String oldest) {
            return new DiscoveryResult("error", null, 0,
                    "Nedá sa vypočítať staršia sezóna z: " + oldest);
        }

        static DiscoveryResult alreadyExists(String ref) {
            return new DiscoveryResult("exists", ref, 0,
                    "Sezóna " + ref + " už existuje pre toto mesto");
        }

        static DiscoveryResult found(String ref, long meetings) {
            return new DiscoveryResult("found", ref, meetings,
                    "Nájdených " + meetings + " zasadnutí pre " + ref);
        }

        static DiscoveryResult notFound(String ref) {
            return new DiscoveryResult("not_found", ref, 0,
                    "Žiadne dáta pre " + ref + " v dostupných zdrojoch");
        }
    }

    public DiscoveryResult discoverOlderSeason(Town town) {
        // Load town with its seasons
        Town managedTown = txTemplate.execute(status -> {
            Town t = townRepository.findByRefWithSeasons(town.getRef());
            t.getSeasons().size(); // force init
            return t;
        });

        Set<Season> townSeasons = managedTown.getSeasons();

        Season oldest = townSeasons.stream()
                .min(Comparator.comparing(Season::getRef))
                .orElse(null);

        if (oldest == null) return DiscoveryResult.noSeasons();

        String prevRef = calculatePreviousSeason(oldest.getRef());
        if (prevRef == null) return DiscoveryResult.cannotCalculate(oldest.getRef());

        // Check if town already has this season
        if (townSeasons.stream().anyMatch(s -> s.getRef().equals(prevRef))) {
            return DiscoveryResult.alreadyExists(prevRef);
        }

        // Find or create season in global table
        Season season = txTemplate.execute(status -> {
            Season existing = seasonRepository.findByRef(prevRef);
            if (existing != null) return existing;
            Season newSeason = new Season();
            newSeason.setRef(prevRef);
            newSeason.setName(prevRef);
            seasonRepository.save(newSeason);
            log.info("Created new season: {}", prevRef);
            return newSeason;
        });

        // Reset cache so the new season is visible
        cacheManager.resetSeasonsMap();

        // Sync members first so votes can be matched
        councilMemberSyncService.syncCouncilMembers(managedTown, Set.of(season));

        // Then sync meetings
        Map<InstitutionType, List<Institution>> institutionsMap = cacheManager.loadInstitutionsMap();
        meetingSyncService.syncSeasonMeetings(managedTown, season, institutionsMap);

        // Check results
        long meetingCount = meetingRepository.countMeetingsByTownAndSeason(town.getRef(), prevRef);

        if (meetingCount > 0) {
            // Link season to town
            txTemplate.executeWithoutResult(status -> {
                Town t = townRepository.findByRefWithSeasons(town.getRef());
                Season s = seasonRepository.findByRef(prevRef);
                t.addSeason(s);
            });
            cacheManager.resetTownsMap();
            return DiscoveryResult.found(prevRef, meetingCount);
        } else {
            return DiscoveryResult.notFound(prevRef);
        }
    }

    static String calculatePreviousSeason(String seasonRef) {
        try {
            String[] parts = seasonRef.split("-");
            int startYear = Integer.parseInt(parts[0]);
            int endYear = Integer.parseInt(parts[1]);
            int duration = endYear - startYear;
            if (duration <= 0) return null;
            return (startYear - duration) + "-" + startYear;
        } catch (Exception e) {
            return null;
        }
    }
}
