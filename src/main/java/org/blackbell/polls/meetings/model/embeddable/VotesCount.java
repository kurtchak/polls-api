package org.blackbell.polls.meetings.model.embeddable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.Embeddable;

@Embeddable
public class VotesCount {
    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "absent")
    private int absent;

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(int absent) {
        this.absent = absent;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "for")
    private int votedFor;

    public int getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "against")
    private int votedAgainst;

    public int getVotedAgainst() {
        return votedAgainst;
    }

    public void setVotedAgainst(int votedAgainst) {
        this.votedAgainst = votedAgainst;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "abstain")
    private int abstain;

    public int getAbstain() {
        return abstain;
    }

    public void setAbstain(int abstain) {
        this.abstain = abstain;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "not")
    private int notVoted;

    public int getNotVoted() {
        return notVoted;
    }

    public void setNotVoted(int notVoted) {
        this.notVoted = notVoted;
    }

    public VotesCount() {
    }
}