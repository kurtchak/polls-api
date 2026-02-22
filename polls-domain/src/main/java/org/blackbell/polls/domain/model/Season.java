package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.domain.model.enums.Source;

import jakarta.persistence.*;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Season extends NamedEntity {

    @Column(length = 500)
    private String syncError;

    @Enumerated(EnumType.STRING)
    private Source dataSource;

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
    }

    public Source getDataSource() {
        return dataSource;
    }

    public void setDataSource(Source dataSource) {
        this.dataSource = dataSource;
    }

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

        return ref.equals(season.ref);

    }

    @Override
    public int hashCode() {
        return ref.hashCode();
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
