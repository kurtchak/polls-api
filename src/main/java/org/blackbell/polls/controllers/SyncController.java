package org.blackbell.polls.controllers;

import org.blackbell.polls.source.SyncAgent;
import org.blackbell.polls.sync.SyncProgress;
import org.blackbell.polls.sync.SyncStatusDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SyncController {

    private final SyncProgress syncProgress;
    private final SyncAgent syncAgent;

    public SyncController(SyncProgress syncProgress, SyncAgent syncAgent) {
        this.syncProgress = syncProgress;
        this.syncAgent = syncAgent;
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
        boolean alreadyRunning = syncAgent.isRunning();
        syncAgent.triggerSync(town);
        String target = town != null ? town : "all towns";
        String status = alreadyRunning ? "queued" : "started";
        String message = alreadyRunning
                ? "Sync already running, " + target + " queued after current sync"
                : "Synchronization started for " + target;
        return ResponseEntity.ok(Map.of("status", status, "message", message));
    }
}
