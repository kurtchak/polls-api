package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class AgendaItem {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.Meeting.class, Views.Polls.class, Views.Poll.class, Views.Votes.class, Views.AgendaItem.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Meeting.class, Views.Polls.class, Views.Poll.class, Views.Votes.class, Views.Agenda.class, Views.AgendaItem.class})
    private String name;

    @JsonProperty(value = "idBodProgramu")
    private String extId;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.AgendaItem.class, Views.Votes.class})
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @JsonView(value = {Views.AgendaItem.class})
    @JsonIgnore
    @OneToMany(mappedBy = "agendaItem", cascade = CascadeType.ALL)
    private List<Poll> polls;

    @JsonView(value = {Views.AgendaItem.class, Views.Poll.class})
    @OneToMany(mappedBy = "agendaItem", cascade = CascadeType.ALL)
    private List<AgendaItemAttachment> attachments;

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

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }

    public List<Poll> getPolls() {
        return polls;
    }

    public void setPolls(List<Poll> polls) {
        this.polls = polls;
    }

    public List<AgendaItemAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AgendaItemAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addPoll(Poll poll) {
        if (polls == null) {
            polls = new ArrayList<>();
        }
        polls.add(poll);
        poll.setAgendaItem(this);
    }

    public void addAgendaItemAttachment(AgendaItemAttachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
        attachment.setAgendaItem(this);
    }
}
