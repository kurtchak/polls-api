package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.List;

/**
 * Created by kurtcha on 11.3.2018.
 */
@Entity
public class ClubMember {

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne
    @JoinColumn(name = "council_member_id")
    private CouncilMember councilMember;

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
}
