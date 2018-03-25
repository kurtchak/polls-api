package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kurtcha on 11.3.2018.
 */
@Entity
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Clubs.class, Views.Club.class, Views.ClubMembers.class})
    private String ref;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Clubs.class, Views.Club.class, Views.ClubMembers.class})
    private String name;

    @JsonView(value = {Views.Club.class})
    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ClubParty> clubParties;

    @JsonIgnore
    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ClubMember> clubMembers;

    @JsonView(value = {Views.Club.class})
    @ManyToOne
    @JoinColumn(name = "town_id")
    private Town town;

    @JsonView(value = {Views.Club.class})
    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setParties(List<ClubParty> clubParties) {
        this.clubParties = clubParties;
    }

    public List<ClubMember> getClubMembers() {
        return clubMembers;
    }

    public void setClubMembers(List<ClubMember> clubMembers) {
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

    public List<ClubParty> getClubParties() {
        return clubParties;
    }

    public void setClubParties(List<ClubParty> clubParties) {
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
            clubMembers = new ArrayList<>();
        }
        clubMembers.add(clubMember);
        clubMember.setClub(this);
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
