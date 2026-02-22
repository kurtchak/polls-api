package org.blackbell.polls.sync;

import java.time.Instant;

public class SyncStatusDTO {
    private boolean running;
    private String currentTown;
    private String currentSeason;
    private String currentPhase;
    private int totalMeetings;
    private int processedMeetings;
    private Instant startedAt;
    private Instant lastCompletedAt;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getCurrentTown() {
        return currentTown;
    }

    public void setCurrentTown(String currentTown) {
        this.currentTown = currentTown;
    }

    public String getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(String currentSeason) {
        this.currentSeason = currentSeason;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public int getTotalMeetings() {
        return totalMeetings;
    }

    public void setTotalMeetings(int totalMeetings) {
        this.totalMeetings = totalMeetings;
    }

    public int getProcessedMeetings() {
        return processedMeetings;
    }

    public void setProcessedMeetings(int processedMeetings) {
        this.processedMeetings = processedMeetings;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastCompletedAt() {
        return lastCompletedAt;
    }

    public void setLastCompletedAt(Instant lastCompletedAt) {
        this.lastCompletedAt = lastCompletedAt;
    }
}
