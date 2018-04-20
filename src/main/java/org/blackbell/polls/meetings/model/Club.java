package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.ClubPartiesSerializer;
import org.blackbell.polls.meetings.model.common.BaseEntity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by kurtcha on 11.3.2018.
 */
@Entity
public class Club extends BaseEntity {

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Clubs.class, Views.Club.class, Views.ClubMembers.class})
    private String ref;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Clubs.class, Views.Club.class, Views.ClubMembers.class})
    private String name;

    @JsonView(value = {Views.Club.class})
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    @JsonSerialize(using = ClubPartiesSerializer.class)
    @JsonProperty("parties")
    private Set<ClubParty> clubParties;

    @JsonView(value = {Views.Club.class})
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
//    @JsonSerialize(using = ClubMembersSerializer.class)
    @JsonProperty("members")
    private Set<ClubMember> clubMembers;

    @JsonView(value = {Views.Club.class})
    @ManyToOne
    @JoinColumn(name = "town_id")
    private Town town;

    @JsonView(value = {Views.Club.class})
    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    public Set<ClubMember> getClubMembers() {
        return clubMembers;
    }

    public void setClubMembers(Set<ClubMember> clubMembers) {
        this.clubMembers = clubMembers;
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

    public Set<ClubParty> getClubParties() {
        return clubParties;
    }

    public void setClubParties(Set<ClubParty> clubParties) {
        this.clubParties = clubParties;
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public void addClubMember(ClubMember clubMember) {
        if (clubMembers == null) {
            clubMembers = new HashSet<>();
        }
        clubMembers.add(clubMember);
        clubMember.setClub(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Club)) return false;

        Club club = (Club) o;

        return getId() == club.getId();

    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    @Override
    public String toString() {
        return "Club{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", clubParties=" + clubParties +
                ", clubMembers=" + clubMembers +
                ", town=" + town +
                ", season=" + season +
                '}';
    }
}
