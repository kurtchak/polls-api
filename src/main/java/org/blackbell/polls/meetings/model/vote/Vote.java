package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "voted", discriminatorType = DiscriminatorType.STRING)
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @JsonView(value = Views.Poll.class)
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

    @JsonView(value = {Views.Poll.class, Views.CouncilMember.class})
    @Enumerated(EnumType.STRING)
    @Column(name = "voted")
    private VoteChoiceEnum voted;

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

    public VoteChoiceEnum getVoted() {
        return voted;
    }

    public void setVoted(VoteChoiceEnum voted) {
        this.voted = voted;
    }
}
