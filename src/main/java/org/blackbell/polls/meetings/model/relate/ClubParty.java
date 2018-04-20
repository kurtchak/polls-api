package org.blackbell.polls.meetings.model.relate;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.ClubPartySerializer;
import org.blackbell.polls.meetings.model.Club;
import org.blackbell.polls.meetings.model.Party;
import org.blackbell.polls.meetings.model.Season;
import org.blackbell.polls.meetings.model.common.BaseEntity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Created by kurtcha on 11.3.2018.
 */
@Entity
@JsonSerialize(using = ClubPartySerializer.class)
public class ClubParty extends BaseEntity {

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Club.class})
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "party_id")
    private Party party;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClubParty)) return false;

        ClubParty clubParty = (ClubParty) o;

        return id == clubParty.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
