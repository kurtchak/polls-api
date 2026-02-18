package org.blackbell.polls.controllers;

import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.service.SeasonService;
import org.blackbell.polls.source.SeasonDiscoveryService;
import org.blackbell.polls.source.SyncCacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class SeasonsController {

    private final SeasonService seasonService;
    private final SeasonDiscoveryService discoveryService;
    private final SyncCacheManager cacheManager;

    public SeasonsController(SeasonService seasonService,
                             SeasonDiscoveryService discoveryService,
                             SyncCacheManager cacheManager) {
        this.seasonService = seasonService;
        this.discoveryService = discoveryService;
        this.cacheManager = cacheManager;
    }

    @RequestMapping("/{city}/{institution}/seasons")
    public List<Map<String, Object>> seasons(@PathVariable String city,
                                              @PathVariable String institution) {
        return seasonService.getSeasonsWithCounts(city);
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
