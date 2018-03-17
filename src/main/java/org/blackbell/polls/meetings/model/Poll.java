package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.vote.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
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

    @JsonIgnore
    private String townRef;

    @JsonIgnore
    private String seasonRef;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private Institution institution;

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

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "absent")
    private int absent;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "votedFor")
    private int votedFor;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "votedAgainst")
    private int votedAgainst;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "abstain")
    private int abstain;

    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @JsonProperty(value = "notVoted")
    private int notVoted;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.CouncilMember.class})
    @ManyToOne
    @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoteFor> votesFor;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoteAgainst> votesAgainst;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NoVote> noVotes;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Abstain> abstains;

    @JsonView(value = Views.Poll.class)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

    public String getTownRef() {
        return townRef;
    }

    public void setTownRef(String townRef) {
        this.townRef = townRef;
    }

    public String getSeasonRef() {
        return seasonRef;
    }

    public void setSeasonRef(String seasonRef) {
        this.seasonRef = seasonRef;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
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

    public void addVoteFor(CouncilMember member) {
        if (votesFor == null) {
            votesFor = new ArrayList<VoteFor>();
        }
        votesFor.add(new VoteFor(this, member));
    }
    public void addVoteAgainst(CouncilMember member) {
        if (votesAgainst == null) {
            votesAgainst = new ArrayList<VoteAgainst>();
        }
        votesAgainst.add(new VoteAgainst(this, member));
    }
    public void addNoVote(CouncilMember member) {
        if (noVotes == null) {
            noVotes = new ArrayList<NoVote>();
        }
        noVotes.add(new NoVote(this, member));
    }
    public void addAbstain(CouncilMember member) {
        if (abstains == null) {
            abstains = new ArrayList<Abstain>();
        }
        abstains.add(new Abstain(this, member));
    }
    public void addAbsent(CouncilMember member) {
        if (absents == null) {
            absents = new ArrayList<Absent>();
        }
        absents.add(new Absent(this, member));
    }

    public int getVoters() {
        return voters;
    }

    public void setVoters(int voters) {
        this.voters = voters;
    }

    public int getAbsent() {
        return absent;
    }

    public void setAbsent(int absent) {
        this.absent = absent;
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

    public int getAbstain() {
        return abstain;
    }

    public void setAbstain(int abstain) {
        this.abstain = abstain;
    }

    public int getNotVoted() {
        return notVoted;
    }

    public void setNotVoted(int notVoted) {
        this.notVoted = notVoted;
    }

    // TODO: When is Passed?
    @JsonView(value = {Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @Transient
    public VoteResult getResult() {
        return getVotedFor() > getVotedAgainst() ? VoteResult.PASSED : VoteResult.REJECTED;
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
                ", absent=" + absent +
                ", votedFor=" + votedFor +
                ", votedAgainst=" + votedAgainst +
                ", abstain=" + abstain +
                ", notVoted=" + notVoted +
                ", agendaItem=" + agendaItem +
                '}';
    }
}
