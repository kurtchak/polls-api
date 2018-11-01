package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.domain.model.relate.PartyNominee;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.Set;

/**
 * Created by Ján Korčák on 2.4.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Party extends NamedEntity {

    @JsonView(value = {Views.Party.class})
    private String description;

    @JsonView(value = {Views.Parties.class, Views.Party.class})
    private String logo;

    @JsonIgnore
    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PartyNominee> partyNominees;

    @JsonView(value = {Views.Parties.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Parties.class, Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.Club.class})
    public String getName() {
        return name;
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

}
