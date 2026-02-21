package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.SeasonRepository;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.sync.SyncEventBroadcaster;
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
    private final TownRepository townRepository;
    private final SyncCacheManager cacheManager;
    private final SyncEventBroadcaster eventBroadcaster;

    public SeasonSyncService(DataSourceResolver resolver, SeasonRepository seasonRepository,
                             TownRepository townRepository, SyncCacheManager cacheManager,
                             SyncEventBroadcaster eventBroadcaster) {
        this.resolver = resolver;
        this.seasonRepository = seasonRepository;
        this.townRepository = townRepository;
        this.cacheManager = cacheManager;
        this.eventBroadcaster = eventBroadcaster;
    }

    @Transactional
    public void syncSeasons(Town town) {
        try {
            Town managedTown = townRepository.findByRefWithSeasons(town.getRef());

            List<DataSourceResolver.SourcedItem<Season>> sourcedSeasons =
                    resolver.resolveAndAggregate(managedTown, DataOperation.SEASONS,
                            di -> di.loadSeasons(managedTown));
            log.info(Constants.MarkerSync, "RETRIEVED SEASONS: {}", sourcedSeasons);
            eventBroadcaster.emit("SUCCESS", managedTown.getRef(), "seasons",
                    "Found " + sourcedSeasons.size() + " seasons");

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

            // Link synced seasons to this town
            for (DataSourceResolver.SourcedItem<Season> si : sourcedSeasons) {
                Season managed = seasonRepository.findByRef(si.item().getRef());
                if (managed != null) {
                    managedTown.addSeason(managed);
                }
            }

            // Refresh caches
            cacheManager.resetSeasonsMap();
            cacheManager.resetTownsMap();
        } catch (Exception e) {
            log.error(Constants.MarkerSync, "An error occured during the {}s seasons synchronization.", town.getName(), e);
        }
    }
}
