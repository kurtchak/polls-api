package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.BaseEntity;
import org.blackbell.polls.domain.model.enums.VoteChoice;

import jakarta.persistence.*;

/**
 * Created by Ján Korčák on 18.3.2017.
 * email: korcak@esten.sk
 */
@Entity
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@MappedSuperclass
//@DiscriminatorColumn(name = "voted", discriminatorType = DiscriminatorType.STRING)
//@DiscriminatorOptions(force = true)
//@DiscriminatorFormula(
//        "CASE WHEN voted == VOTED_FOR THEN 'Voted' " +
//                " WHEN txt_value IS NOT NULL THEN 'TEXT' end"
//)
public class Vote extends BaseEntity {

    @JsonView(value = Views.Poll.class)
    @ManyToOne
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

    @JsonView(value = Views.Poll.class)
    @Column(name = "voter_name")
    private String voterName;

    @JsonView(value = {Views.CouncilMember.class, Views.Votes.class})
    @Enumerated(EnumType.STRING)
    @Column(name = "voted")
    private VoteChoice voted;

    @JsonView(value = {Views.CouncilMember.class, Views.Votes.class})
    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    public CouncilMember getCouncilMember() {
        return councilMember;
    }

    public void setCouncilMember(CouncilMember councilMember) {
        this.councilMember = councilMember;
    }

    public String getVoterName() {
        return voterName;
    }

    public void setVoterName(String voterName) {
        this.voterName = voterName;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public VoteChoice getVoted() {
        return voted;
    }

    public void setVoted(VoteChoice voted) {
        this.voted = voted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vote)) return false;

        Vote vote = (Vote) o;

        if (voterName != null ? !voterName.equals(vote.voterName) : vote.voterName != null) return false;
        if (getCouncilMember() != null ? !getCouncilMember().equals(vote.getCouncilMember()) : vote.getCouncilMember() != null) return false;
        if (getVoted() != vote.getVoted()) return false;
        return getPoll() != null ? getPoll().equals(vote.getPoll()) : vote.getPoll() == null;
    }

    @Override
    public int hashCode() {
        int result = voterName != null ? voterName.hashCode() : 0;
        result = 31 * result + (getVoted() != null ? getVoted().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id=" + id +
                ", voted=" + voted +
                ", councilMember=" + councilMember +
                '}';
    }
}
