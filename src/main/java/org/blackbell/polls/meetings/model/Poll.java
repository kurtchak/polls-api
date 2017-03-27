package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Poll {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @JsonView(value = {Views.Polls.class, Views.CouncilMember.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.CouncilMember.class})
    private String name;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.CouncilMember.class})
    @ManyToOne
    @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<Vote> votes;

    @JsonView(value = Views.Poll.class)
    @Transient
    private int votedFor;
    @JsonView(value = Views.Poll.class)
    @Transient
    private int votedAgainst;
    @JsonView(value = Views.Poll.class)
    @Transient
    private int notVoted;
    @JsonView(value = Views.Poll.class)
    @Transient
    private int abstain;
    @JsonView(value = Views.Poll.class)
    @Transient
    private int absent;
    @JsonView(value = Views.Poll.class)
    @Transient
    private VoteResult result;

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

    public AgendaItem getAgendaItem() {
        return agendaItem;
    }

    public void setAgendaItem(AgendaItem agendaItem) {
        this.agendaItem = agendaItem;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }

    public int getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    public int getVotedAgainst() {
        return votedAgainst;
    }

    public void setVotedAgainst(int votedAgainst) {
        this.votedAgainst = votedAgainst;
    }

    public int getNotVoted() {
        return notVoted;
    }

    public void setNotVoted(int notVoted) {
        this.notVoted = notVoted;
    }

    public int getAbstain() {
        return abstain;
    }

    public void setAbstain(int abstain) {
        this.abstain = abstain;
    }

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(int absent) {
        this.absent = absent;

    }

    public VoteResult getResult() {
        return result;
    }

    public void setResult(VoteResult result) {
        this.result = result;
    }

}
