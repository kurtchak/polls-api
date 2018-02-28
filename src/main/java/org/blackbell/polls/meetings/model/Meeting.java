package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.properties.SeasonAsPropertySerializer;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

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

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "season_id")
    @JsonSerialize(using = SeasonAsPropertySerializer.class)
    private Season season;

    @JsonView(value = {Views.Meeting.class, Views.Meetings.class, Views.Poll.class, Views.Polls.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date date;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private List<AgendaItem> agendaItems;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private List<MeetingAttachment> attachments;
    private String extId;

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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<AgendaItem> getAgendaItems() {
        return agendaItems;
    }

    public void setAgendaItems(List<AgendaItem> agendaItems) {
        this.agendaItems = agendaItems;
    }

    public List<MeetingAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MeetingAttachment> attachments) {
        this.attachments = attachments;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getExtId() {
        return extId;
    }
}
