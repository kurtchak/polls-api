package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles synchronization of seasons (volebné obdobia) from data sources.
 */
@Component
public class SeasonSyncService {
    private static final Logger log = LoggerFactory.getLogger(SeasonSyncService.class);

    private final DataSourceResolver resolver;
    private final SeasonRepository seasonRepository;
    private final SyncCacheManager cacheManager;

    public SeasonSyncService(DataSourceResolver resolver, SeasonRepository seasonRepository,
                             SyncCacheManager cacheManager) {
        this.resolver = resolver;
        this.seasonRepository = seasonRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public void syncSeasons(Town town) {
        try {
            List<DataSourceResolver.SourcedItem<Season>> sourcedSeasons =
                    resolver.resolveAndAggregate(town, DataOperation.SEASONS,
                            di -> di.loadSeasons(town));
            log.info(Constants.MarkerSync, "RETRIEVED SEASONS: {}", sourcedSeasons);

            List<Season> formerSeasons = seasonRepository.findAll();
            log.info(Constants.MarkerSync, "FORMER SEASONS: {}", formerSeasons);

            sourcedSeasons.stream()
                    .filter(si -> !formerSeasons.contains(si.item()))
                    .forEach(si -> {
                        Season season = si.item();
                        season.setDataSource(si.source());
                        log.info("Adding new season: {} (source: {})", season, si.source());
                        seasonRepository.save(season);
                    });

            // Refresh seasons cache after saving new ones
            cacheManager.resetSeasonsMap();
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {}s seasons synchronization.", town.getName(), e);
        }
    }
}
