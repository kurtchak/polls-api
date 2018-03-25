package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.PoliticianClubSerializer;
import org.blackbell.polls.meetings.json.serializers.PoliticianPartyNomineesSerializer;
import org.blackbell.polls.meetings.json.serializers.properties.SeasonAsPropertySerializer;
import org.blackbell.polls.meetings.model.vote.Vote;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
//@JsonSerialize(using = CouncilMemberSerializer.class)
public class CouncilMember {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(unique = true)
    @JsonView(value = {Views.CouncilMembers.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String ref;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String name;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String titles;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String picture;

    @JsonIgnore
    private String extId;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String email;

    @JsonView(value = {Views.CouncilMember.class, Views.Poll.class})
    private String phone;

//    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @OneToMany(mappedBy = "councilMember", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JsonSerialize(using = PoliticianClubSerializer.class)
    @JsonIgnore
    private List<ClubMember> clubMembers;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @JsonProperty("memberOf")
    public ClubMember getActualClubMember() {
        if (clubMembers != null) {
            Calendar cal = Calendar.getInstance();
            for (ClubMember cm : clubMembers) {
                String[] range = cm.getClub().getSeason().getName().split("-");
                if (Integer.valueOf(range[0]) <= cal.get(Calendar.YEAR)
                        && Integer.valueOf(range[1]) >= cal.get(Calendar.YEAR)) {
                    return cm;
                }
            }
        }
        return null;
    }

    @JsonView(value = {Views.CouncilMember.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String otherFunctions;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    @JsonSerialize(using = SeasonAsPropertySerializer.class)
    private Season season;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @OneToMany(mappedBy = "councilMember", cascade = CascadeType.ALL)
    @JsonSerialize(using = PoliticianPartyNomineesSerializer.class)
    @JsonProperty("nominee")
    private List<PartyNominee> partyNominees;

//    @JsonView(value = Views.CouncilMember.class)
    @JsonIgnore
    @OneToMany(mappedBy = "councilMember")
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

    public String getTitles() {
        return titles;
    }

    public void setTitles(String titles) {
        this.titles = titles;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<ClubMember> getClubMembers() {
        return clubMembers;
    }

    public void setClubMembers(List<ClubMember> clubMembers) {
        this.clubMembers = clubMembers;
    }

    public String getOtherFunctions() {
        return otherFunctions;
    }

    public void setOtherFunctions(String otherFunctions) {
        this.otherFunctions = otherFunctions;
    }

    public List<PartyNominee> getPartyNominees() {
        return partyNominees;
    }

    public void setPartyNominees(List<PartyNominee> partyNominees) {
        this.partyNominees = partyNominees;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CouncilMember that = (CouncilMember) o;

        if (id != that.id) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CouncilMember{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", titles='" + titles + '\'' +
                ", picture='" + picture + '\'' +
                ", extId='" + extId + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", otherFunctions='" + otherFunctions + '\'' +
                ", season=" + season +
                '}';
    }

    public void addClubMember(ClubMember clubMember) {
        if (clubMembers == null) {
            clubMembers = new ArrayList<>();
        }
        clubMembers.add(clubMember);
        clubMember.setCouncilMember(this);
    }
}
