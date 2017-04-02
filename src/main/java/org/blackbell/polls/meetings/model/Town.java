package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ján Korčák on 6.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Town {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    @Column(unique = true)
    private String ref;
    private String name;

    public Town() {}

    public Town(String ref, String name) {
        this.ref = ref;
        this.name = name;
    }

    @OneToMany(mappedBy = "town", cascade = CascadeType.ALL)
    private List<Season> seasons;

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

    public List<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
    }

    public List<Season> getSeasons(Institution institution) {
        List<Season> institutionSeasons = null;
        if (seasons != null) {
            institutionSeasons = new ArrayList<>();
            for (Season season : seasons) {
                if (institution.equals(season.getInstitution())) {
                    institutionSeasons.add(season);
                }
            }
        }
        return institutionSeasons;
    }

    public void addSeasons(List<Season> seasons) {
        if (this.seasons == null) {
            this.seasons = seasons;
        } else {
            seasons.addAll(seasons);
        }
    }
}
