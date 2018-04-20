package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.common.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class AgendaItemAttachment extends BaseEntity {

    @JsonView(value = {Views.Poll.class, Views.AgendaItem.class, Views.Meeting.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Poll.class, Views.AgendaItem.class, Views.Meeting.class})
    private String name;

    @JsonView(value = {Views.Poll.class, Views.AgendaItem.class, Views.Meeting.class})
    private String source;

    @ManyToOne @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

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

    public AgendaItem getAgendaItem() {
        return agendaItem;
    }

    public void setAgendaItem(AgendaItem agendaItem) {
        this.agendaItem = agendaItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgendaItemAttachment)) return false;

        AgendaItemAttachment that = (AgendaItemAttachment) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
