package org.blackbell.polls.controllers;

import org.blackbell.polls.source.SyncOrchestrator;
import org.blackbell.polls.sync.SyncProgress;
import org.blackbell.polls.sync.SyncStatusDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SyncController {

    private final SyncProgress syncProgress;
    private final SyncOrchestrator syncOrchestrator;

    public SyncController(SyncProgress syncProgress, SyncOrchestrator syncOrchestrator) {
        this.syncProgress = syncProgress;
        this.syncOrchestrator = syncOrchestrator;
    }

    @GetMapping("/sync/status")
    public SyncStatusDTO syncStatus() {
        return syncProgress.getStatus();
    }

    @PostMapping("/sync/trigger")
    public ResponseEntity<Map<String, String>> triggerSync() {
        return triggerSync(null);
    }

    @PostMapping("/sync/trigger/{town}")
    public ResponseEntity<Map<String, String>> triggerSync(@PathVariable(required = false) String town) {
        boolean alreadyRunning = syncOrchestrator.isRunning();
        syncOrchestrator.triggerSync(town);
        String target = town != null ? town : "all towns";
        String status = alreadyRunning ? "queued" : "started";
        String message = alreadyRunning
                ? "Sync already running, " + target + " queued after current sync"
                : "Synchronization started for " + target;
        return ResponseEntity.ok(Map.of("status", status, "message", message));
    }

    @PostMapping("/sync/trigger/{town}/{season}")
    public ResponseEntity<Map<String, String>> triggerSync(@PathVariable String town, @PathVariable String season) {
        boolean alreadyRunning = syncOrchestrator.isRunning();
        syncOrchestrator.triggerSync(town, season);
        String target = town + "/" + season;
        String status = alreadyRunning ? "queued" : "started";
        String message = alreadyRunning
                ? "Sync already running, " + target + " queued after current sync"
                : "Synchronization started for " + target;
        return ResponseEntity.ok(Map.of("status", status, "message", message));
    }
}
