package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.api.serializers.VoteListSerializer;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.MajorityType;
import org.blackbell.polls.domain.model.enums.Source;
import org.blackbell.polls.domain.model.enums.VoteResult;

import jakarta.persistence.*;
import java.util.Set;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Poll extends NamedEntity {

    @JsonProperty(value = "idBodProgramu")
    private String extAgendaItemId;

    @JsonProperty(value = "route")
    private String extPollRouteId;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "note")
    @Column(length = 1000)
    private String note;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "voters")
    private int voters;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "markedAsIrrelevant")
    @Transient
    private boolean markedAsIrrelevant;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Enumerated(EnumType.STRING)
    private Source dataSource;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Embedded
    private VotesCount votesCount;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    @JsonProperty("votes")
    @JsonSerialize(using = VoteListSerializer.class)
//    @Enumerated(EnumType.STRING)
//    @ElementCollection
//    @MapKeyEnumerated(EnumType.STRING)
//    @MapKeyColumn(name = "voted")
    private Set<Vote> votes;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.Votes.class})
    @ManyToOne
    @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

    @JsonView(value = {Views.Polls.class, Views.Votes.class, Views.AgendaItem.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.Votes.class, Views.AgendaItem.class})
    public String getName() {
        return name;
    }

    public String getExtAgendaItemId() {
        return extAgendaItemId;
    }

    public void setExtAgendaItemId(String extAgendaItemId) {
        this.extAgendaItemId = extAgendaItemId;
    }

    public String getExtPollRouteId() {
        return extPollRouteId;
    }

    public void setExtPollRouteId(String extPollRouteId) {
        this.extPollRouteId = extPollRouteId;
    }

    public AgendaItem getAgendaItem() {
        return agendaItem;
    }

    public void setAgendaItem(AgendaItem agendaItem) {
        this.agendaItem = agendaItem;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getVoters() {
        return voters;
    }

    public void setVoters(int voters) {
        this.voters = voters;
    }

    public Set<Vote> getVotes() {
        return votes;
    }

    public void setVotes(Set<Vote> votes) {
        this.votes = votes;
    }

    public Source getDataSource() {
        return dataSource;
    }

    public void setDataSource(Source dataSource) {
        this.dataSource = dataSource;
    }

    public VotesCount getVotesCount() {
        return votesCount;
    }

    public boolean isMarkedAsIrrelevant() {
        return markedAsIrrelevant;
    }

    public void setMarkedAsIrrelevant(boolean markedAsIrrelevant) {
        this.markedAsIrrelevant = markedAsIrrelevant;
    }

    public void setVotesCount(VotesCount votesCount) {
        this.votesCount = votesCount;
    }

    /**
     * Typ potrebnej väčšiny detekovaný z názvu bodu programu.
     * Podľa zákona č. 369/1990 Zb. o obecnom zriadení.
     */
    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Transient
    public MajorityType getMajorityType() {
        if (agendaItem == null) {
            return MajorityType.SIMPLE_MAJORITY;
        }
        return MajorityType.detectFromAgendaItemName(agendaItem.getName());
    }

    /**
     * Výsledok hlasovania podľa typu potrebnej väčšiny:
     * - SIMPLE_MAJORITY: >50% prítomných (§12 ods. 7)
     * - THREE_FIFTHS_PRESENT: 3/5 prítomných (§12 ods. 7 — VZN)
     * - THREE_FIFTHS_ALL: 3/5 všetkých (§13 ods. 8 — prelomenie veta)
     * - ABSOLUTE_MAJORITY: >50% všetkých (§18a a ďalšie)
     */
    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class, Views.Votes.class})
    @Transient
    public VoteResult getResult() {
        if (votesCount == null) {
            return null;
        }
        int present = voters - votesCount.getAbsent();
        if (present <= 0) {
            return null;
        }
        MajorityType majorityType = getMajorityType();
        return switch (majorityType) {
            case THREE_FIFTHS_PRESENT -> votesCount.getVotedFor() * 5 > present * 3
                    ? VoteResult.PASSED : VoteResult.REJECTED;
            case THREE_FIFTHS_ALL -> votesCount.getVotedFor() * 5 > voters * 3
                    ? VoteResult.PASSED : VoteResult.REJECTED;
            case ABSOLUTE_MAJORITY -> votesCount.getVotedFor() * 2 > voters
                    ? VoteResult.PASSED : VoteResult.REJECTED;
            default -> votesCount.getVotedFor() * 2 > present
                    ? VoteResult.PASSED : VoteResult.REJECTED;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Poll)) return false;

        Poll poll = (Poll) o;

        if (!getName().equals(poll.getName())) return false;
        return getAgendaItem().equals(poll.getAgendaItem());

    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getAgendaItem().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Poll{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", extAgendaItemId='" + extAgendaItemId + '\'' +
                ", extPollRouteId='" + extPollRouteId + '\'' +
                ", note='" + note + '\'' +
                ", voters=" + voters +
                ", markedAsIrrelevant=" + markedAsIrrelevant +
                ", absent=" + votesCount.getAbsent() +
                ", votedFor=" + votesCount.getVotedFor() +
                ", votedAgainst=" + votesCount.getVotedAgainst() +
                ", abstain=" + votesCount.getAbstain() +
                ", notVoted=" + votesCount.getNotVoted() +
                ", agendaItem=" + agendaItem +
                '}';
    }
}
