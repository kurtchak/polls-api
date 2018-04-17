package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.model.common.BaseEntity;

import javax.persistence.*;
import java.util.Set;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Party extends BaseEntity {

    @JsonView(value = {Views.Parties.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Parties.class, Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Club.class})
    private String name;

    @JsonView(value = {Views.Party.class})
    private String description;

    @JsonView(value = {Views.Parties.class, Views.Party.class})
    private String logo;

    @JsonIgnore
    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY)
    private Set<PartyNominee> partyNominees;

    public Party() {}

    public Party(String name) {
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Set<PartyNominee> getPartyNominees() {
        return partyNominees;
    }

    public void setPartyNominees(Set<PartyNominee> partyNominees) {
        this.partyNominees = partyNominees;
    }

    // TODO: is it right
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Party)) return false;

        Party party = (Party) o;

        return id == party.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
