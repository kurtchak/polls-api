package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class MeetingAttachment {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @Column(unique = true)
    private String ref;
    private String name;
    private String source;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    public MeetingAttachment() {
    }

    public MeetingAttachment(String name, Meeting meeting, String ref, String source) {
        this.ref = ref;
        this.name = name;
        this.source = source;
        this.meeting = meeting;
    }

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}
