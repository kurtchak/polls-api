package org.blackbell.polls.controllers;

import org.blackbell.polls.source.SyncOrchestrator;
import org.blackbell.polls.sync.SyncEvent;
import org.blackbell.polls.sync.SyncEventBroadcaster;
import org.blackbell.polls.sync.SyncProgress;
import org.blackbell.polls.sync.SyncStatusDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SyncController {

    private final SyncProgress syncProgress;
    private final SyncOrchestrator syncOrchestrator;
    private final SyncEventBroadcaster syncEventBroadcaster;

    public SyncController(SyncProgress syncProgress, SyncOrchestrator syncOrchestrator,
                           SyncEventBroadcaster syncEventBroadcaster) {
        this.syncProgress = syncProgress;
        this.syncOrchestrator = syncOrchestrator;
        this.syncEventBroadcaster = syncEventBroadcaster;
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

    @GetMapping(value = "/sync/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return syncEventBroadcaster.subscribe();
    }

    @GetMapping("/sync/last-run")
    public Map<String, Object> getLastRun() {
        List<SyncEvent> lastRunEvents = syncEventBroadcaster.getLastRunEvents();
        SyncStatusDTO status = syncProgress.getStatus();

        int meetingsSynced = 0;
        int meetingsFailed = 0;
        int meetingsSkipped = 0;
        int townsSynced = 0;

        for (SyncEvent e : lastRunEvents) {
            if ("meeting".equals(e.phase())) {
                if ("SUCCESS".equals(e.level())) meetingsSynced++;
                else if ("ERROR".equals(e.level())) meetingsFailed++;
                else if ("WARN".equals(e.level())) meetingsSkipped++;
            }
            if ("start".equals(e.phase()) && "INFO".equals(e.level()) && e.town() != null) {
                townsSynced++;
            }
        }

        String startedAt = null;
        String completedAt = null;
        long durationMs = 0;

        if (!lastRunEvents.isEmpty()) {
            startedAt = lastRunEvents.get(0).timestamp().toString();
            SyncEvent last = lastRunEvents.get(lastRunEvents.size() - 1);
            if ("complete".equals(last.phase())) {
                completedAt = last.timestamp().toString();
                durationMs = last.timestamp().toEpochMilli() - lastRunEvents.get(0).timestamp().toEpochMilli();
            }
        }

        // If sync is currently running, use status startedAt
        if (status.isRunning() && status.getStartedAt() != null) {
            startedAt = status.getStartedAt().toString();
        }

        List<Map<String, Object>> eventMaps = lastRunEvents.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.id());
            m.put("timestamp", e.timestamp().toString());
            m.put("level", e.level());
            m.put("town", e.town() != null ? e.town() : "");
            m.put("season", e.season() != null ? e.season() : "");
            m.put("phase", e.phase());
            m.put("message", e.message());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("events", eventMaps);
        result.put("startedAt", startedAt);
        result.put("completedAt", completedAt);
        result.put("durationMs", durationMs);
        result.put("townsSynced", townsSynced);
        result.put("meetingsSynced", meetingsSynced);
        result.put("meetingsFailed", meetingsFailed);
        result.put("meetingsSkipped", meetingsSkipped);
        return result;
    }
}
