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
        if (syncAgent.isRunning()) {
            return ResponseEntity.status(409)
                    .body(Map.of("status", "already_running", "message", "Synchronization is already in progress"));
        }
        syncAgent.triggerSync(town);
        String message = town != null
                ? "Synchronization started for town: " + town
                : "Synchronization started for all towns";
        return ResponseEntity.ok(Map.of("status", "started", "message", message));
    }
}
