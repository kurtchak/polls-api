package org.blackbell.polls.sync;

import java.util.Date;

public class SyncStatusDTO {
    private boolean running;
    private String currentTown;
    private String currentSeason;
    private String currentPhase;
    private int totalMeetings;
    private int processedMeetings;
    private Date startedAt;
    private Date lastCompletedAt;

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

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getLastCompletedAt() {
        return lastCompletedAt;
    }

    public void setLastCompletedAt(Date lastCompletedAt) {
        this.lastCompletedAt = lastCompletedAt;
    }
}
