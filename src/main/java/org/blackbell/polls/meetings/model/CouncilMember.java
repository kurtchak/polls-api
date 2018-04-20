package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.json.serializers.CouncilMemberSerializer;
import org.blackbell.polls.meetings.json.serializers.PoliticianClubSerializer;
import org.blackbell.polls.meetings.json.serializers.SeasonPropertySerializer;
import org.blackbell.polls.meetings.model.common.EntityWithReference;
import org.blackbell.polls.meetings.model.enums.MemberType;
import org.blackbell.polls.meetings.model.relate.ClubMember;
import org.blackbell.polls.meetings.model.relate.PartyNominee;

import javax.persistence.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
@JsonSerialize(using = CouncilMemberSerializer.class)
public class CouncilMember extends EntityWithReference {

    @JsonIgnore
    private String extId;

//    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @OneToMany(mappedBy = "councilMember", cascade = CascadeType.ALL)
//    @JsonSerialize(using = PoliticianClubSerializer.class)
    @JsonIgnore
    private Set<ClubMember> clubMembers;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @JsonProperty("club")
    @JsonSerialize(using = PoliticianClubSerializer.class)
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

    @JsonView(value = {Views.CouncilMember.class, Views.PartyNominees.class, Views.ClubMembers.class, Views.Club.class})
    private String otherFunctions;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", insertable = false, updatable = false)
    @JsonSerialize(using = SeasonPropertySerializer.class)
    private Season season;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "town_id", insertable = false, updatable = false)
//    @JsonSerialize(using = SeasonAsPropertySerializer.class)
    private Town town;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @OneToOne
    @JoinColumn(name = "politician_id")
    private Politician politician;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @JsonProperty("position")
    @JoinColumn(name = "member_type")
    @Enumerated(EnumType.STRING)
    private MemberType memberType;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.PartyNominees.class})
    private String description;

//    @JsonView(value = Views.CouncilMember.class)
//    @JsonIgnore
//    @OneToMany(mappedBy = "councilMember")
//    private List<Vote> votes;

    @JsonView(value = {Views.CouncilMembers.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class, Views.Club.class})
    public String getRef() {
        return ref;
    }

    public String getExtId() {
        return extId;
    }

    public void setExtId(String extId) {
        this.extId = extId;
    }

    public Set<ClubMember> getClubMembers() {
        return clubMembers;
    }

    public void setClubMembers(Set<ClubMember> clubMembers) {
        this.clubMembers = clubMembers;
    }

    public String getOtherFunctions() {
        return otherFunctions;
    }

    public void setOtherFunctions(String otherFunctions) {
        this.otherFunctions = otherFunctions;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
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

    public Politician getPolitician() {
        return politician;
    }

    public void setPolitician(Politician politician) {
        this.politician = politician;
    }

    public MemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(MemberType memberType) {
        this.memberType = memberType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

//    public Set<Vote> getVotes() {
//        return votes;
//    }
//
//    public void setVotes(Set<Vote> votes) {
//        this.votes = votes;
//    }

    public void addClubMember(ClubMember clubMember) {
        if (clubMembers == null) {
            clubMembers = new HashSet<>();
        }
        clubMembers.add(clubMember);
        clubMember.setCouncilMember(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouncilMember)) return false;

        CouncilMember that = (CouncilMember) o;

        if (!season.equals(that.season)) return false;
        if (!town.equals(that.town)) return false;
        if (institution != that.institution) return false;
        return politician.equals(that.politician);

    }

    @Override
    public int hashCode() {
        int result = season.hashCode();
        result = 31 * result + town.hashCode();
        result = 31 * result + institution.hashCode();
        result = 31 * result + politician.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CouncilMember{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", extId='" + extId + '\'' +
                ", clubMembers=" + clubMembers +
                ", otherFunctions='" + otherFunctions + '\'' +
                ", season=" + season +
                ", town=" + town +
                ", institution=" + institution +
                ", politician=" + politician +
                ", memberType=" + memberType +
                ", description='" + description + '\'' +
                '}';
    }

    public Set<PartyNominee> getPartyNominees() {
        return politician.getPartyNominees(); // TODO: create structure/map identified by season
    }
}
