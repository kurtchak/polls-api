package org.blackbell.polls.controllers;

import org.blackbell.polls.sync.SyncProgress;
import org.blackbell.polls.sync.SyncStatusDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SyncController {

    private final SyncProgress syncProgress;

    public SyncController(SyncProgress syncProgress) {
        this.syncProgress = syncProgress;
    }

    @GetMapping("/sync/status")
    public SyncStatusDTO syncStatus() {
        return syncProgress.getStatus();
    }
}
