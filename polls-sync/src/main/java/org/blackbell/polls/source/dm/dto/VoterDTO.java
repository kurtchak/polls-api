package org.blackbell.polls.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kurtcha on 5.3.2018.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoterDTO {

    @JsonProperty(value = "Name")
    private String name;

    @JsonProperty(value = "za")
    private boolean votedFor;

    @JsonProperty(value = "proti")
    private boolean votedAgainst;

    @JsonProperty(value = "nehlasoval")
    private boolean notVoted;

    @JsonProperty(value = "zdrzalSa")
    private boolean abstain;

    @JsonProperty(value = "nepritomny")
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
