package org.blackbell.polls.meetings.model.vote;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.Poll;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 28.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ABSTAIN")
public class Abstain extends Vote {

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne
    @JoinColumn(name = "poll_id", updatable = false, insertable = false)
    private Poll poll;

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }
}
