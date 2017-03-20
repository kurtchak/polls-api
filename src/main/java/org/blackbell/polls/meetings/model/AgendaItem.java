package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class AgendaItem {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @Column(unique = true)
    private String ref;
    private String name;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @JsonIgnore
    @OneToMany(mappedBy = "agendaItem", cascade = CascadeType.ALL)
    private List<Poll> polls;

    @JsonIgnore
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
}
