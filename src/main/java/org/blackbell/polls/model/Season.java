package org.blackbell.polls.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.model.common.NamedEntity;

import javax.persistence.Entity;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Season extends NamedEntity {

    @JsonView(value = {Views.Seasons.class, Views.Poll.class, Views.CouncilMember.class, Views.Towns.class, Views.Club.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Seasons.class, Views.Towns.class, Views.Club.class})
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Season)) return false;

        Season season = (Season) o;

        return id == season.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "Season{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
