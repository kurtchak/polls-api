package org.blackbell.polls.meetings.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Ján Korčák on 28.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@DiscriminatorValue("VOTED_FOR")
public class VoteFor extends Vote {
}
