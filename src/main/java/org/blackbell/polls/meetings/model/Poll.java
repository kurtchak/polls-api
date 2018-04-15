package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.VoteListSerializer;

import javax.persistence.*;
import java.util.Set;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Poll {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.Polls.class, Views.Votes.class, Views.AgendaItem.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.Votes.class, Views.AgendaItem.class})
    private String name;

    @JsonProperty(value = "idBodProgramu")
    private String extAgendaItemId;

    @JsonProperty(value = "route")
    private String extPollRouteId;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "note")
    private String note;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "voters")
    private int voters;

    @JsonView(value = {Views.Polls.class, Views.AgendaItem.class})
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public VotesCount getVotesCount() {
        return votesCount;
    }

    public void setVotesCount(VotesCount votesCount) {
        this.votesCount = votesCount;
    }

    // TODO: When is Passed?
    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class, Views.Votes.class})
    @Transient
    public VoteResult getResult() {
        return votesCount != null && votesCount.getVotedFor() > votesCount.getVotedAgainst() ? VoteResult.PASSED : VoteResult.REJECTED;
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
                ", absent=" + votesCount.getAbsent() +
                ", votedFor=" + votesCount.getVotedFor() +
                ", votedAgainst=" + votesCount.getVotedAgainst() +
                ", abstain=" + votesCount.getAbstain() +
                ", notVoted=" + votesCount.getNotVoted() +
                ", agendaItem=" + agendaItem +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Poll)) return false;

        Poll poll = (Poll) o;

        return getId() == poll.getId();

    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }
}
