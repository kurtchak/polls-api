package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.source.Source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ján Korčák on 6.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Town extends NamedEntity {

    @Enumerated(EnumType.STRING)
    private Source source;

    @JsonView(value = {Views.Towns.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSyncDate;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "town_season",
            joinColumns = @JoinColumn(name = "town_id"),
            inverseJoinColumns = @JoinColumn(name = "season_id"))
    private Set<Season> seasons = new HashSet<>();

    @JsonView(value = {Views.Towns.class, Views.Club.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Towns.class, Views.Club.class})
    public String getName() {
        return name;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Date lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }

    public Set<Season> getSeasons() {
        return seasons;
    }

    public void addSeason(Season season) {
        seasons.add(season);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Town)) return false;

        Town town = (Town) o;

        return getRef() == town.getRef();

    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    @Override
    public String toString() {
        return "Town{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", source=" + source +
                ", lastSyncDate=" + lastSyncDate +
                '}';
    }
}
