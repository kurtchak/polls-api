package org.blackbell.polls.source;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.sync.SyncProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.Set;

/**
 * Orchestrates the synchronization process. Delegates to specialized services.
 * No self-injection needed — all transactional calls happen on injected service beans
 * or via TransactionTemplate.
 */
@Component
public class SyncOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(SyncOrchestrator.class);

    private final SeasonSyncService seasonSyncService;
    private final CouncilMemberSyncService councilMemberSyncService;
    private final MeetingSyncService meetingSyncService;
    private final SyncCacheManager cacheManager;
    private final SyncProgress syncProgress;
    private final TownRepository townRepository;
    private final TransactionTemplate txTemplate;

    public SyncOrchestrator(SeasonSyncService seasonSyncService,
                            CouncilMemberSyncService councilMemberSyncService,
                            MeetingSyncService meetingSyncService,
                            SyncCacheManager cacheManager,
                            SyncProgress syncProgress,
                            TownRepository townRepository,
                            PlatformTransactionManager txManager) {
        this.seasonSyncService = seasonSyncService;
        this.councilMemberSyncService = councilMemberSyncService;
        this.meetingSyncService = meetingSyncService;
        this.cacheManager = cacheManager;
        this.syncProgress = syncProgress;
        this.townRepository = townRepository;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Scheduled(fixedRateString = "${sync.fixed-rate-ms}", initialDelayString = "${sync.initial-delay-ms}")
    public synchronized void sync() {
        syncAll();
    }

    @Async
    public synchronized void triggerSync(String townRef) {
        if (townRef != null) {
            cacheManager.resetTownsMap();
            Town town = cacheManager.getTown(townRef);
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
        Set<String> townsRefs = cacheManager.getTownsRefs();
        cacheManager.loadInstitutionsMap();
        log.info(Constants.MarkerSync, "Synchronization started");
        syncProgress.startSync();

        if (townsRefs.isEmpty()) {
            log.info(Constants.MarkerSync, "No town to sync");
        }

        try {
            townsRefs.forEach(townRef -> {
                log.info("town: {}", townRef);
                Town town = cacheManager.getTown(townRef);
                syncTown(town);
            });
        } finally {
            syncProgress.finishSync();
            log.info(Constants.MarkerSync, "Synchronization finished");
        }
    }

    private void syncSingleTown(Town town) {
        cacheManager.loadInstitutionsMap();
        log.info(Constants.MarkerSync, "Manual sync started for town: {}", town.getRef());
        syncProgress.startSync();

        try {
            cacheManager.resetSeasonsMap();
            syncTown(town);
        } finally {
            syncProgress.finishSync();
            log.info(Constants.MarkerSync, "Manual sync finished for town: {}", town.getRef());
        }
    }

    private void syncTown(Town town) {
        syncProgress.startTown(town.getRef());
        seasonSyncService.syncSeasons(town);
        councilMemberSyncService.syncCouncilMembers(town);

        cacheManager.getSeasonsRefs().forEach(seasonRef ->
                meetingSyncService.syncSeasonMeetings(town, cacheManager.getSeason(seasonRef),
                        cacheManager.getInstitutionsMap()));

        txTemplate.executeWithoutResult(status -> {
            town.setLastSyncDate(new Date());
            townRepository.save(town);
        });
        log.info("Updated lastSyncDate for town: {}", town.getName());
        log.info(Constants.MarkerSync, "Synchronization finished for town: {}", town.getRef());
    }
}
