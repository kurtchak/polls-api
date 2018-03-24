package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Poll {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    private String name;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date date;

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

    @JsonView(value = {Views.Poll.class})
    @Embedded
    private Votes votes;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class})
    @ManyToOne(fetch = FetchType.LAZY)
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public Votes getVotes() {
        return votes;
    }

    public void setVotes(Votes votes) {
        this.votes = votes;
    }

    public VotesCount getVotesCount() {
        return votesCount;
    }

    public void setVotesCount(VotesCount votesCount) {
        this.votesCount = votesCount;
    }

    // TODO: When is Passed?
    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
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
}
