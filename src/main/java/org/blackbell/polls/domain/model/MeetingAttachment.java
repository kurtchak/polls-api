package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.NamedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class MeetingAttachment extends NamedEntity {

    @JsonView(value = {Views.Meeting.class, Views.Poll.class})
    private String source;

    @ManyToOne
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @JsonView(value = Views.Meeting.class)
    public String getRef() {
        return ref;
    }

    @JsonView(value = Views.Meeting.class)
    public String getName() {
        return name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetingAttachment)) return false;

        MeetingAttachment that = (MeetingAttachment) o;

        return getId() == that.getId();

    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }
}
