package org.blackbell.polls.controllers;

import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.source.SeasonDiscoveryService;
import org.blackbell.polls.source.SyncCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminSeasonController {

    private final SeasonDiscoveryService discoveryService;
    private final SyncCacheManager cacheManager;

    public AdminSeasonController(SeasonDiscoveryService discoveryService,
                                 SyncCacheManager cacheManager) {
        this.discoveryService = discoveryService;
        this.cacheManager = cacheManager;
    }

    @PostMapping("/{city}/{institution}/discover-older-season")
    public ResponseEntity<SeasonDiscoveryService.DiscoveryResult> discoverOlderSeason(
            @PathVariable String city, @PathVariable String institution) {
        Town town = cacheManager.getTown(city);
        if (town == null) {
            return ResponseEntity.notFound().build();
        }
        SeasonDiscoveryService.DiscoveryResult result = discoveryService.discoverOlderSeason(town);
        return ResponseEntity.ok(result);
    }
}
