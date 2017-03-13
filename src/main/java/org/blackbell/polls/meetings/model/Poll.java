package org.blackbell.polls.meetings.model;

import java.util.List;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
public class Poll {
    private long id;
    private String order;
    private String name;

    private List<CouncilMember> votedFor;
    private List<CouncilMember> votedAgainst;
    private List<CouncilMember> notVoted;
    private List<CouncilMember> abstain;
    private List<CouncilMember> absent;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CouncilMember> getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(List<CouncilMember> votedFor) {
        this.votedFor = votedFor;
    }

    public List<CouncilMember> getVotedAgainst() {
        return votedAgainst;
    }

    public void setVotedAgainst(List<CouncilMember> votedAgainst) {
        this.votedAgainst = votedAgainst;
    }

    public List<CouncilMember> getNotVoted() {
        return notVoted;
    }

    public void setNotVoted(List<CouncilMember> notVoted) {
        this.notVoted = notVoted;
    }

    public List<CouncilMember> getAbstain() {
        return abstain;
    }

    public void setAbstain(List<CouncilMember> abstain) {
        this.abstain = abstain;
    }

    public List<CouncilMember> getAbsent() {
        return absent;
    }

    public void setAbsent(List<CouncilMember> absent) {
        this.absent = absent;
    }
}
