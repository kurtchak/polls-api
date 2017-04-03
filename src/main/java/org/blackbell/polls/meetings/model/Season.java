package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"town_id", "ref"})})
public class Season {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @JsonView(value = Views.Poll.class)
    private String ref;

    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "town_id")
    private Town town;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private Institution institution;

    @JsonIgnore
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private List<Meeting> meetings;

    @JsonIgnore
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private List<CouncilMember> members;

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

    public List<Meeting> getMeetings() {
        return meetings;
    }

    public void setMeetings(List<Meeting> meetings) {
        this.meetings = meetings;
    }

    public List<CouncilMember> getMembers() {
        return members;
    }

    public void setMembers(List<CouncilMember> members) {
        this.members = members;
    }
}
