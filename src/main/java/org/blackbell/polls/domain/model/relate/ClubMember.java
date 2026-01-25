package org.blackbell.polls.domain.model.relate;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.Club;
import org.blackbell.polls.domain.model.CouncilMember;
import org.blackbell.polls.domain.model.common.BaseEntity;
import org.blackbell.polls.domain.model.enums.ClubFunction;

import jakarta.persistence.*;

/**
 * Created by kurtcha on 11.3.2018.
 */
@Entity
public class ClubMember extends BaseEntity {

    @JsonView(value = {Views.CouncilMember.class, Views.Poll.class, Views.CouncilMembers.class})
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "club_id")
    private Club club;

    @JsonView(value = {Views.ClubMembers.class, Views.Club.class})
    @ManyToOne
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.ClubMembers.class, Views.Club.class})
    @Enumerated(EnumType.STRING)
    private ClubFunction clubFunction;

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
        return getCouncilMember().equals(that.getCouncilMember());
    }

    @Override
    public int hashCode() {
        int result = getClub().hashCode();
        result = 31 * result + getCouncilMember().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClubMember{" +
                "club=" + club +
                ", councilMember=" + councilMember +
                ", clubFunction=" + clubFunction +
                '}';
    }
}
