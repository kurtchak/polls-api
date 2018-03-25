package org.blackbell.polls.meetings.model.vote;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.Poll;
import org.blackbell.polls.meetings.model.VoteChoice;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 28.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ABSTAIN")
public class Abstain extends Vote {

    @JsonView(value = {Views.CouncilMember.class, Views.Votes.class})
    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    public Abstain() {
        setVoted(VoteChoice.ABSTAIN);
    }

    public Abstain(Poll poll, CouncilMember cm) {
        this();
        setPoll(poll);
        setCouncilMember(cm);
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }
}
