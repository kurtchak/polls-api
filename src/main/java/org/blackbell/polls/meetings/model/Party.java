package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Party {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @JsonView(value = {Views.Parties.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Parties.class, Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Club.class})
    private String name;

    @JsonView(value = {Views.Party.class})
    private String description;

    @JsonView(value = {Views.Parties.class, Views.Party.class})
    private String logo;

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    @JsonIgnore
    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY)
    private List<PartyNominee> partyNominees;

    public Party() {}

    public Party(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public List<PartyNominee> getPartyNominees() {
        return partyNominees;
    }

    public void setPartyNominees(List<PartyNominee> partyNominees) {
        this.partyNominees = partyNominees;
    }

    // TODO: is it right
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Party)) return false;

        Party party = (Party) o;

        if (!name.equals(party.name)) return false;
        return season.equals(party.season);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + season.hashCode();
        return result;
    }
}
