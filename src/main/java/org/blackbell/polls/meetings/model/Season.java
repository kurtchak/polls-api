package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"town_id", "name", "institution"})})
public class Season {
    @JsonIgnore
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @JsonView(value = {Views.Seasons.class, Views.Poll.class, Views.CouncilMember.class, Views.Towns.class, Views.Club.class})
    private String ref;

    @JsonView(value = {Views.Seasons.class, Views.Towns.class, Views.Club.class})
    private String name;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "town_id")
    private Town town;

    @JsonView(value = {Views.Seasons.class, Views.Towns.class, Views.Club.class})
    @Enumerated(EnumType.STRING)
    private Institution institution;

    @JsonView(value = {Views.Towns.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date lastSyncDate;

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

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Date lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Season)) return false;

        Season season = (Season) o;

        if (!name.equals(season.name)) return false;
        if (!town.equals(season.town)) return false;
        return institution == season.institution;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + town.hashCode();
        result = 31 * result + institution.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Season{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", town=" + town +
                ", institution=" + institution +
                ", lastSyncDate=" + lastSyncDate +
                '}';
    }
}
