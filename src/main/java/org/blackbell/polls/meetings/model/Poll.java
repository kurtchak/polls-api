package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@Entity
@NamedNativeQueries({
    @NamedNativeQuery(name = "Poll.findPolls", query = "select p from Poll p where p.agendaItem.meeting.town.name = :town")
})
public class Poll {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @Column(unique = true)
    private String ref;
    private String name;

    @ManyToOne
    @JoinColumn(name = "agenda_item_id")
    private AgendaItem agendaItem;

    @JsonIgnore
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL)
    private List<Vote> votes;

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

    public AgendaItem getAgendaItem() {
        return agendaItem;
    }

    public void setAgendaItem(AgendaItem agendaItem) {
        this.agendaItem = agendaItem;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }
}
