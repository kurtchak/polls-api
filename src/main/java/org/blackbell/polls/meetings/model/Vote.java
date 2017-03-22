package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Entity
//@FilterDefs({
//        @FilterDef(name = "votedFor", defaultCondition = "voted = 'VOTED_FOR'"),
//        @FilterDef(name = "votedAgainst", defaultCondition = "voted = 'VOTED_AGAINST'"),
//        @FilterDef(name = "notVoted", defaultCondition = "voted = 'NOT_VOTED'"),
//        @FilterDef(name = "abstain", defaultCondition = "voted = 'ABSTAIN'"),
//        @FilterDef(name = "absent", defaultCondition = "voted = 'ABSENT'")
//})
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @JsonView(value = Views.Poll.class)
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

    @JsonView(value = Views.Poll.class)
    @Enumerated(EnumType.STRING)
    @Column(name = "voted")
    private VoteEnum voted;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public CouncilMember getCouncilMember() {
        return councilMember;
    }

    public void setCouncilMember(CouncilMember councilMember) {
        this.councilMember = councilMember;
    }

    public VoteEnum getVoted() {
        return voted;
    }

    public void setVoted(VoteEnum voted) {
        this.voted = voted;
    }
}
