package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.vote.Vote;

import javax.persistence.*;
import java.util.List;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class CouncilMember {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @Column(unique = true)
    @JsonView(value = {Views.CouncilMembers.class, Views.Poll.class})
    private String ref;
    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    private String name;
    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    private String picture;
    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    private String email;

    @JsonView(value = Views.CouncilMember.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private Season season;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class})
    @OneToMany(mappedBy = "councilMember", cascade = CascadeType.ALL)
    private List<PartyNominee> partyNominees;

    @JsonView(value = Views.CouncilMember.class)
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

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
