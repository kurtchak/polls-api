package org.blackbell.polls.sync;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SyncProgress {
    private boolean running;
    private String currentTown;
    private String currentSeason;
    private String currentPhase;
    private Instant startedAt;
    private Instant lastCompletedAt;
    private final AtomicInteger totalMeetings = new AtomicInteger(0);
    private final AtomicInteger processedMeetings = new AtomicInteger(0);

    public synchronized void startSync() {
        running = true;
        startedAt = Instant.now();
        currentTown = null;
        currentSeason = null;
        currentPhase = "seasons";
        totalMeetings.set(0);
        processedMeetings.set(0);
    }

    public synchronized void finishSync() {
        running = false;
        lastCompletedAt = Instant.now();
        currentTown = null;
        currentSeason = null;
        currentPhase = null;
    }

    public synchronized void startTown(String townRef) {
        currentTown = townRef;
        currentPhase = "members";
    }

    public synchronized void startSeason(String townRef, String seasonRef, int meetingsCount) {
        currentTown = townRef;
        currentSeason = seasonRef;
        currentPhase = "meetings";
        totalMeetings.set(meetingsCount);
        processedMeetings.set(0);
    }

    public void meetingProcessed() {
        processedMeetings.incrementAndGet();
    }

    public synchronized SyncStatusDTO getStatus() {
        SyncStatusDTO dto = new SyncStatusDTO();
        dto.setRunning(running);
        dto.setCurrentTown(currentTown);
        dto.setCurrentSeason(currentSeason);
        dto.setCurrentPhase(currentPhase);
        dto.setTotalMeetings(totalMeetings.get());
        dto.setProcessedMeetings(processedMeetings.get());
        dto.setStartedAt(startedAt);
        dto.setLastCompletedAt(lastCompletedAt);
        return dto;
    }
}
