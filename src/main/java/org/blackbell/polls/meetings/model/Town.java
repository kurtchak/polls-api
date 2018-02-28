package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.json.Views;
import org.blackbell.polls.meetings.source.Source;

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
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @JsonView(value = {Views.Towns.class})
    @Column(unique = true)
    private String ref;

    @JsonView(value = {Views.Towns.class})
    private String name;

    @Enumerated(EnumType.STRING)
    private Source source;

    public Town() {}

    public Town(String ref, String name, Source source) {
        this.ref = ref;
        this.name = name;
        this.source = source;
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

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
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
