package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.properties.SeasonAsPropertySerializer;

import javax.persistence.*;
import java.util.*;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Meeting {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.Meetings.class, Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Meeting.class, Views.Meetings.class, Views.Poll.class, Views.Polls.class, Views.AgendaItem.class})
    private String name;

    private String extId;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "season_id", insertable = false, updatable = false)
    @JsonSerialize(using = SeasonAsPropertySerializer.class)
    private Season season;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "town_id", insertable = false, updatable = false)
    private Town town;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    @JsonView(value = {Views.Meeting.class, Views.Meetings.class, Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date date;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private Set<AgendaItem> agendaItems;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private Set<MeetingAttachment> attachments;

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
