package org.blackbell.polls.meetings.model.relate;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.Club;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.common.BaseEntity;
import org.blackbell.polls.meetings.model.common.EntityWithReference;
import org.blackbell.polls.meetings.model.enums.ClubFunction;

import javax.persistence.*;

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

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
