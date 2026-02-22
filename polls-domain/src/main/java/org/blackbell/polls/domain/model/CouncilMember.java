package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.api.serializers.CouncilMemberSerializer;
import org.blackbell.polls.domain.api.serializers.PoliticianClubSerializer;
import org.blackbell.polls.domain.model.common.EntityWithReference;
import org.blackbell.polls.domain.model.relate.ClubMember;
import org.blackbell.polls.domain.model.enums.Source;
import org.blackbell.polls.domain.model.relate.PartyNominee;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
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
    public ClubMember getClubMember() {
        return clubMembers != null ? clubMembers.stream().findFirst().orElse(null) : null;
    }

    @JsonView(value = {Views.CouncilMember.class, Views.PartyNominees.class, Views.ClubMembers.class, Views.Club.class})
    @Column(length = 2000)
    private String otherFunctions;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", insertable = true, updatable = false)
    private Season season;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "town_id", insertable = true, updatable = false)
//    @JsonSerialize(using = SeasonAsPropertySerializer.class)
    private Town town;

    @JsonView(value = {Views.Meeting.class, Views.Poll.class, Views.CouncilMember.class, Views.AgendaItem.class})
    @ManyToOne @JoinColumn(name = "institution_id", insertable = true, updatable = false)
    private Institution institution;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "politician_id")
    private Politician politician;

    @Enumerated(EnumType.STRING)
    private Source dataSource;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Source getDataSource() {
        return dataSource;
    }

    public void setDataSource(Source dataSource) {
        this.dataSource = dataSource;
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
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CouncilMember)) return false;

        CouncilMember member = (CouncilMember) o;

        if (!Objects.equals(getSeason(), member.getSeason())) return false;
        if (!Objects.equals(getTown(), member.getTown())) return false;
        if (!Objects.equals(getInstitution(), member.getInstitution())) return false;
        return Objects.equals(getPolitician(), member.getPolitician());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSeason(), getTown(), getInstitution(), getPolitician());
    }

    @Override
    public String toString() {
        return "CouncilMember{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", extId='" + extId + '\'' +
                ", otherFunctions='" + otherFunctions + '\'' +
                ", season=" + season +
                ", town=" + town +
                ", institution=" + institution +
                ", politician=" + politician +
                ", description='" + description + '\'' +
                '}';
    }

    public Set<PartyNominee> getPartyNominees() {
        return politician.getPartyNominees();
    }
}
