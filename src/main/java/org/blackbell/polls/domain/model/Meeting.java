package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.api.serializers.InstitutionPropertySerializer;
import org.blackbell.polls.domain.api.serializers.SeasonPropertySerializer;
import org.blackbell.polls.domain.api.serializers.TownPropertySerializer;
import org.blackbell.polls.domain.model.common.NamedEntity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Meeting extends NamedEntity {

    private String extId;

    @JsonView(value = {Views.Meetings.class, Views.Meeting.class, Views.Poll.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "season_id")
    @JsonSerialize(using = SeasonPropertySerializer.class)
    private Season season;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "town_id")
    @JsonSerialize(using = TownPropertySerializer.class)
    private Town town;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "institution_id")
    @JsonSerialize(using = InstitutionPropertySerializer.class)
    private Institution institution;

    @JsonView(value = {Views.Meetings.class, Views.Meeting.class, Views.Poll.class, Views.Polls.class, Views.Votes.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @JsonFormat(pattern = "dd.MM.yyyy")
    @Temporal(TemporalType.DATE)
    private Date date;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private Set<AgendaItem> agendaItems;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private Set<MeetingAttachment> attachments;

    @Column(length = 1000)
    private String syncError;

    @JsonView(value = {Views.Meetings.class, Views.Poll.class, Views.Polls.class, Views.Votes.class, Views.AgendaItem.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Meetings.class, Views.Meeting.class, Views.Poll.class, Views.Polls.class, Views.Votes.class, Views.AgendaItem.class})
    public String getName() {
        return name;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
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

    public Set<AgendaItem> getAgendaItems() {
        return agendaItems;
    }

    public void setAgendaItems(Set<AgendaItem> agendaItems) {
        this.agendaItems = agendaItems;
    }

    public Set<MeetingAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<MeetingAttachment> attachments) {
        this.attachments = attachments;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getExtId() {
        return extId;
    }

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
    }

    public void addAgendaItem(AgendaItem agendaItem) {
        if (agendaItems == null) {
            agendaItems = new HashSet<>();
        }
        agendaItem.setMeeting(this);
        agendaItems.add(agendaItem);
    }

    public void addAttachment(MeetingAttachment attachment) {
        if (attachments == null) {
            attachments = new HashSet<>();
        }
        attachment.setMeeting(this);
        attachments.add(attachment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meeting)) return false;

        Meeting meeting = (Meeting) o;

        if (!getSeason().equals(meeting.getSeason())) return false;
        if (!getTown().equals(meeting.getTown())) return false;
        if (!getInstitution().equals(meeting.getInstitution())) return false;
        return getDate().equals(meeting.getDate());

    }

    @Override
    public int hashCode() {
        int result = getSeason().hashCode();
        result = 31 * result + getTown().hashCode();
        result = 31 * result + getInstitution().hashCode();
        result = 31 * result + getDate().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", season=" + season +
                ", town=" + town +
                ", institution=" + institution +
                ", date=" + date +
                ", agendaItems=" + agendaItems +
                ", attachments=" + attachments +
                ", extId='" + extId + '\'' +
                ", agendaItems count = '" + (getAgendaItems() != null ? getAgendaItems().size() : 0) + '\'' +
                '}';
    }
}
