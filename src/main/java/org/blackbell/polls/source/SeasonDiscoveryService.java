package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.Institution;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.MeetingRepository;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SeasonDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SeasonDiscoveryService.class);

    private final SeasonRepository seasonRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingSyncService meetingSyncService;
    private final SyncCacheManager cacheManager;

    public SeasonDiscoveryService(SeasonRepository seasonRepository,
                                  MeetingRepository meetingRepository,
                                  MeetingSyncService meetingSyncService,
                                  SyncCacheManager cacheManager) {
        this.seasonRepository = seasonRepository;
        this.meetingRepository = meetingRepository;
        this.meetingSyncService = meetingSyncService;
        this.cacheManager = cacheManager;
    }

    public record DiscoveryResult(String status, String seasonRef, long meetingCount, String message) {
        static DiscoveryResult noSeasons() {
            return new DiscoveryResult("error", null, 0, "Žiadne sezóny v databáze");
        }

        static DiscoveryResult cannotCalculate(String oldest) {
            return new DiscoveryResult("error", null, 0,
                    "Nedá sa vypočítať staršia sezóna z: " + oldest);
        }

        static DiscoveryResult alreadyExists(String ref) {
            return new DiscoveryResult("exists", ref, 0,
                    "Sezóna " + ref + " už existuje");
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
        List<Season> allSeasons = seasonRepository.findAll();

        Season oldest = allSeasons.stream()
                .min(Comparator.comparing(Season::getRef))
                .orElse(null);

        if (oldest == null) return DiscoveryResult.noSeasons();

        String prevRef = calculatePreviousSeason(oldest.getRef());
        if (prevRef == null) return DiscoveryResult.cannotCalculate(oldest.getRef());

        boolean exists = allSeasons.stream().anyMatch(s -> s.getRef().equals(prevRef));
        if (exists) return DiscoveryResult.alreadyExists(prevRef);

        // Create the new season
        Season newSeason = new Season();
        newSeason.setRef(prevRef);
        newSeason.setName(prevRef);
        seasonRepository.save(newSeason);
        log.info("Created new season: {}", prevRef);

        // Reset cache so the new season is visible
        cacheManager.resetSeasonsMap();

        // Try to sync meetings for this season
        Map<InstitutionType, List<Institution>> institutionsMap = cacheManager.loadInstitutionsMap();
        meetingSyncService.syncSeasonMeetings(town, newSeason, institutionsMap);

        // Check results
        long meetingCount = meetingRepository.countMeetingsByTownAndSeason(town.getRef(), prevRef);

        if (meetingCount > 0) {
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
