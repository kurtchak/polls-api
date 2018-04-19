package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.common.BaseEntity;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
@Entity
//@JsonSerialize(using = PoliticianPartyNomineesSerializer.class)
public class PartyNominee extends BaseEntity {

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    @ManyToOne
    @JoinColumn(name = "party_id")
    private Party party;

    @JsonView(value = {Views.PartyNominees.class})
    @ManyToOne
    @JoinColumn(name = "politician_id")
    private Politician politician;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne
    @JoinColumn(name = "town_id")
    private Town town;

    public PartyNominee() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public Politician getPolitician() {
        return politician;
    }

    public void setPolitician(Politician politician) {
        this.politician = politician;
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartyNominee)) return false;

        PartyNominee that = (PartyNominee) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
