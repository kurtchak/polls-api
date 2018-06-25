package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.BaseEntity;
import org.blackbell.polls.domain.model.common.NamedEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class AgendaItem extends NamedEntity {

    @JsonProperty(value = "idBodProgramu")
    private String extId;

    @JsonView(value = {Views.Polls.class, Views.Poll.class, Views.AgendaItem.class, Views.Votes.class})
    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @JsonView(value = {Views.AgendaItem.class})
    @JsonIgnore
    @OneToMany(mappedBy = "agendaItem", cascade = CascadeType.ALL)
    private Set<Poll> polls;

    @JsonView(value = {Views.AgendaItem.class, Views.Poll.class, Views.Meeting.class})
    @OneToMany(mappedBy = "agendaItem", cascade = CascadeType.ALL)
    private Set<AgendaItemAttachment> attachments;

    @JsonView(value = {Views.Meeting.class, Views.Polls.class, Views.Poll.class, Views.Votes.class, Views.Agenda.class, Views.AgendaItem.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Meeting.class, Views.Polls.class, Views.Poll.class, Views.Votes.class, Views.Agenda.class, Views.AgendaItem.class})
    public String getName() {
        return name;
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

    public Set<Poll> getPolls() {
        return polls;
    }

    public void setPolls(Set<Poll> polls) {
        this.polls = polls;
    }

    public Set<AgendaItemAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<AgendaItemAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addPoll(Poll poll) {
        if (polls == null) {
            polls = new HashSet<>();
        }
        poll.setAgendaItem(this);
        polls.add(poll);
    }

    public void addAgendaItemAttachment(AgendaItemAttachment attachment) {
        if (attachments == null) {
            attachments = new HashSet<>();
        }
        attachment.setAgendaItem(this);
        attachments.add(attachment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgendaItem)) return false;

        AgendaItem that = (AgendaItem) o;

        if (!getName().equals(that.getName())) return false;
        return getMeeting().equals(that.getMeeting());

    }

    @Override
    public int hashCode() {
        int result = getName().hashCode();
        result = 31 * result + getMeeting().hashCode();
        return result;
    }
}
