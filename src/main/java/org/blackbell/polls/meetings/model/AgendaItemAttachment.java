package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class AgendaItemAttachment {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @Column(unique = true)
    private String ref;
    private String name;
    private String source;

    @ManyToOne
    @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

    public AgendaItemAttachment() {
    }

    public AgendaItemAttachment(String name, AgendaItem item, String source) {
        this.name = name;
        this.source = source;
        this.agendaItem = item;
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

    public AgendaItem getAgendaItem() {
        return agendaItem;
    }

    public void setAgendaItem(AgendaItem agendaItem) {
        this.agendaItem = agendaItem;
    }
}
