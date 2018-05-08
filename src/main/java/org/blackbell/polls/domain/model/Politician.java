package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.api.serializers.PoliticianPartyNomineesSerializer;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.domain.model.relate.PartyNominee;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Set;

/**
 * Created by Ján Korčák on 21.3.2018.
 * email: korcak@esten.sk
 */
@Entity
public class Politician extends NamedEntity {

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String titles;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String picture;

    @JsonIgnore
    private String extId;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    private String email;

    @JsonView(value = {Views.CouncilMember.class})
    private String phone;

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class})
    @OneToMany(mappedBy = "politician", cascade = CascadeType.ALL)
    @JsonSerialize(using = PoliticianPartyNomineesSerializer.class)
    private Set<PartyNominee> partyNominees;

    @JsonView(value = {Views.CouncilMembers.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.CouncilMembers.class, Views.CouncilMember.class, Views.Poll.class, Views.PartyNominees.class, Views.ClubMembers.class})
    public String getName() {
        return name;
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

    public Set<PartyNominee> getPartyNominees() {
        return partyNominees;
    }

    public void setPartyNominees(Set<PartyNominee> partyNominees) {
        this.partyNominees = partyNominees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Politician that = (Politician) o;

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
        return "Politician{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", titles='" + titles + '\'' +
                ", picture='" + picture + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}
