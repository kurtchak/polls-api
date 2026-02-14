package org.blackbell.polls.sync;

import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SyncProgress {
    private volatile boolean running;
    private volatile String currentTown;
    private volatile String currentSeason;
    private volatile String currentPhase;
    private volatile Date startedAt;
    private volatile Date lastCompletedAt;
    private final AtomicInteger totalMeetings = new AtomicInteger(0);
    private final AtomicInteger processedMeetings = new AtomicInteger(0);

    public void startSync() {
        running = true;
        startedAt = new Date();
        currentTown = null;
        currentSeason = null;
        currentPhase = "seasons";
        totalMeetings.set(0);
        processedMeetings.set(0);
    }

    public void finishSync() {
        running = false;
        lastCompletedAt = new Date();
        currentTown = null;
        currentSeason = null;
        currentPhase = null;
    }

    public void startTown(String townRef) {
        currentTown = townRef;
        currentPhase = "members";
    }

    public void startSeason(String townRef, String seasonRef, int meetingsCount) {
        currentTown = townRef;
        currentSeason = seasonRef;
        currentPhase = "meetings";
        totalMeetings.set(meetingsCount);
        processedMeetings.set(0);
    }

    public void meetingProcessed() {
        processedMeetings.incrementAndGet();
    }

    public SyncStatusDTO getStatus() {
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
