package org.blackbell.polls.domain.model.relate;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Party;
import org.blackbell.polls.domain.model.Politician;
import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.common.BaseEntity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
@Entity
//@JsonSerialize(using = PoliticianPartyNomineesSerializer.class)
public class PartyNominee extends BaseEntity {

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    @ManyToOne(cascade = CascadeType.ALL)
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

        if (!getParty().equals(that.getParty())) return false;
        if (!getPolitician().equals(that.getPolitician())) return false;
        if (!getSeason().equals(that.getSeason())) return false;
        return getTown().equals(that.getTown());
    }

    @Override
    public int hashCode() {
        int result = getParty().hashCode();
        result = 31 * result + getPolitician().hashCode();
        result = 31 * result + getSeason().hashCode();
        result = 31 * result + getTown().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PartyNominee{" +
                "party=" + party +
                ", politician=" + politician +
                ", season=" + season +
                ", town=" + town +
                '}';
    }
}
