package org.blackbell.polls.meetings.model.vote;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.VoteChoice;
import org.hibernate.annotations.DiscriminatorOptions;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "voted", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
public abstract class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = Views.Poll.class)
    @ManyToOne
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

    @JsonView(value = {Views.Poll.class, Views.CouncilMember.class})
    @Enumerated(EnumType.STRING)
    @Column(name = "voted", insertable = false, updatable = false)
    private VoteChoice voted;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public CouncilMember getCouncilMember() {
        return councilMember;
    }

    public void setCouncilMember(CouncilMember councilMember) {
        this.councilMember = councilMember;
    }

    public VoteChoice getVoted() {
        return voted;
    }

    public void setVoted(VoteChoice voted) {
        this.voted = voted;
    }
}
