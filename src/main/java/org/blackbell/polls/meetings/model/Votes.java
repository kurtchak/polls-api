package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.VoteListSerializer;
import org.blackbell.polls.meetings.model.vote.*;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

//@JsonSerialize(using = VoteListSerializer.class)
@Embeddable
public class Votes {

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonProperty("for")
    @JsonSerialize(using = VoteListSerializer.class)
    private List<VoteFor> votesFor;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("against")
    @JsonSerialize(using = VoteListSerializer.class)
    private List<VoteAgainst> votesAgainst;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("not")
    @JsonSerialize(using = VoteListSerializer.class)
    private List<NoVote> noVotes;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("abstain")
    @JsonSerialize(using = VoteListSerializer.class)
    private List<Abstain> abstains;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("absent")
    @JsonSerialize(using = VoteListSerializer.class)
    private List<Absent> absents;

    public Votes() {
    }

    public List<VoteFor> getVotesFor() {
        return votesFor;
    }

    public void setVotesFor(List<VoteFor> votesFor) {
        this.votesFor = votesFor;
    }

    public List<VoteAgainst> getVotesAgainst() {
        return votesAgainst;
    }

    public void setVotesAgainst(List<VoteAgainst> votesAgainst) {
        this.votesAgainst = votesAgainst;
    }

    public List<NoVote> getNoVotes() {
        return noVotes;
    }

    public void setNoVotes(List<NoVote> noVotes) {
        this.noVotes = noVotes;
    }

    public List<Abstain> getAbstains() {
        return abstains;
    }

    public void setAbstains(List<Abstain> abstains) {
        this.abstains = abstains;
    }

    public List<Absent> getAbsents() {
        return absents;
    }

    public void setAbsents(List<Absent> absents) {
        this.absents = absents;
    }

    public void addVoteFor(CouncilMember member) {
        if (votesFor == null) {
            votesFor = new ArrayList<VoteFor>();
        }
        votesFor.add(new VoteFor(null, member));
    }

    public void addVoteAgainst(CouncilMember member) {
        if (votesAgainst == null) {
            votesAgainst = new ArrayList<VoteAgainst>();
        }
        votesAgainst.add(new VoteAgainst(null, member));
    }

    public void addNoVote(CouncilMember member) {
        if (noVotes == null) {
            noVotes = new ArrayList<NoVote>();
        }
        noVotes.add(new NoVote(null, member));
    }

    public void addAbstain(CouncilMember member) {
        if (abstains == null) {
            abstains = new ArrayList<Abstain>();
        }
        abstains.add(new Abstain(null, member));
    }

    public void addAbsent(CouncilMember member) {
        if (absents == null) {
            absents = new ArrayList<Absent>();
        }
        absents.add(new Absent(null, member));
    }
}