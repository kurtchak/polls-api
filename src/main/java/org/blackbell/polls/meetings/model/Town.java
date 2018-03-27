package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.source.Source;

import javax.persistence.*;
import java.util.*;

/**
 * Created by Ján Korčák on 6.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Town {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.Towns.class, Views.Club.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Towns.class, Views.Club.class})
    private String name;

    @Enumerated(EnumType.STRING)
    private Source source;

    @JsonView(value = {Views.Towns.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date lastSyncDate;

    @JsonView(value = {Views.Towns.class})
    @OneToMany(mappedBy = "town", cascade = CascadeType.ALL)
    private Set<Season> seasons;

    public Town() {}

    public Town(String ref, String name, Source source) {
        this.ref = ref;
        this.name = name;
        this.source = source;
    }

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

    public void setSeasons(Set<Season> seasons) {
        this.seasons = seasons;
    }

    public Set<Season> getSeasons(Institution institution) {
        Set<Season> institutionSeasons = null;
        if (seasons != null) {
            institutionSeasons = new HashSet<>();
            for (Season season : seasons) {
                if (institution.equals(season.getInstitution())) {
                    institutionSeasons.add(season);
                }
            }
        }
        return institutionSeasons;
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

    public void addSeasons(Set<Season> seasons) {
        if (this.seasons == null) {
            this.seasons = seasons;
        } else {
            seasons.addAll(seasons);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Town)) return false;

        Town town = (Town) o;

        return ref.equals(town.getRef());

    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }
}
