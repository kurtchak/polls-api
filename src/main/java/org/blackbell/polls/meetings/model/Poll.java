package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.vote.*;

import javax.persistence.*;
import java.util.List;

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

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.CouncilMember.class})
    @ManyToOne
    @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<VoteFor> votesFor;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<VoteAgainst> votesAgainst;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<NoVote> noVotes;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<Abstain> abstains;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<Absent> absents;

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

    public List<VoteFor> getVotesFor() {
        return votesFor;
    }

    public void setVotesFor(List<VoteFor> votesFor) {
        this.votesFor = votesFor;
    }

    public List<VoteAgainst> getVotesAgainst() {
        return votesAgainst;
    }

    public void setVotesAgainst(List<VoteAgainst> votesAgainst) {
        this.votesAgainst = votesAgainst;
    }

    public List<NoVote> getNoVotes() {
        return noVotes;
    }

    public void setNoVotes(List<NoVote> noVotes) {
        this.noVotes = noVotes;
    }

    public List<Abstain> getAbstains() {
        return abstains;
    }

    public void setAbstains(List<Abstain> abstains) {
        this.abstains = abstains;
    }

    public List<Absent> getAbsents() {
        return absents;
    }

    public void setAbsents(List<Absent> absents) {
        this.absents = absents;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Transient
    public int getVotedFor() {
        return votesFor != null ? votesFor.size() : 0;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Transient
    public int getVotedAgainst() {
        return votesAgainst != null ? votesAgainst.size() : 0;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Transient
    public int getNotVoted() {
        return noVotes != null ? noVotes.size() : 0;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Transient
    public int getAbstain() {
        return abstains != null ? abstains.size() : 0;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Transient
    public int getAbsent() {
        return absents != null ? absents.size() : 0;
    }

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @Transient
    public VoteResult getResult() {
        return getVotedFor() > getVotedAgainst() ? VoteResult.PASSED : VoteResult.REJECTED;
    }

}
