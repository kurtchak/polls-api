package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;

/**
 * Created by kurtcha on 11.3.2018.
 */
@Entity
public class ClubMember {

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.CouncilMember.class, Views.Poll.class, Views.CouncilMembers.class})
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "club_id")
    private Club club;

    @JsonView(value = {Views.ClubMembers.class})
    @ManyToOne
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.ClubMembers.class})
    @Enumerated(EnumType.STRING)
    private ClubFunction clubFunction;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public CouncilMember getCouncilMember() {
        return councilMember;
    }

    public void setCouncilMember(CouncilMember councilMember) {
        this.councilMember = councilMember;
    }

    public ClubFunction getClubFunction() {
        return clubFunction;
    }

    public void setClubFunction(ClubFunction clubFunction) {
        this.clubFunction = clubFunction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClubMember)) return false;

        ClubMember that = (ClubMember) o;

        if (!getClub().equals(that.getClub())) return false;
        if (!getCouncilMember().equals(that.getCouncilMember())) return false;
        return getClubFunction() == that.getClubFunction();

    }

    @Override
    public int hashCode() {
        int result = getClub().hashCode();
        result = 31 * result + getCouncilMember().hashCode();
        result = 31 * result + getClubFunction().hashCode();
        return result;
    }
}
