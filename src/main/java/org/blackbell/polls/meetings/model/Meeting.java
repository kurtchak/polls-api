package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

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

    @JsonView(value = {Views.Meetings.class, Views.Poll.class, Views.Polls.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Meeting.class, Views.Meetings.class, Views.Poll.class, Views.Polls.class})
    private String name;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class})
    @ManyToOne @JoinColumn(name = "season_id")
    private Season season;

    @JsonView(value = {Views.Meeting.class, Views.Meetings.class, Views.Poll.class, Views.Polls.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date date;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private List<AgendaItem> agendaItems;

    @JsonView(value = {Views.Meeting.class})
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL)
    private List<MeetingAttachment> attachments;

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
}
