package org.blackbell.polls.meetings.model;

import org.blackbell.polls.meetings.model.Vote;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 28.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@DiscriminatorValue("VOTED_AGAINST")
public class VoteAgainst extends Vote {
}
