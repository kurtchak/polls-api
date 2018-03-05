package org.blackbell.polls.meetings.source.dm.dto;

/**
 * Created by kurtcha on 5.3.2018.
 */
public class VoterDTO {

    private String name;
    private boolean votedFor;
    private boolean votedAgainst;
    private boolean notVoted;
    private boolean abstain;
    private boolean absent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVotedFor() {
        return votedFor;
    }

    public void setVotedFor(boolean votedFor) {
        this.votedFor = votedFor;
    }

    public boolean isVotedAgainst() {
        return votedAgainst;
    }

    public void setVotedAgainst(boolean votedAgainst) {
        this.votedAgainst = votedAgainst;
    }

    public boolean isNotVoted() {
        return notVoted;
    }

    public void setNotVoted(boolean notVoted) {
        this.notVoted = notVoted;
    }

    public boolean isAbstain() {
        return abstain;
    }

    public void setAbstain(boolean abstain) {
        this.abstain = abstain;
    }

    public boolean isAbsent() {
        return absent;
    }

    public void setAbsent(boolean absent) {
        this.absent = absent;
    }
}
