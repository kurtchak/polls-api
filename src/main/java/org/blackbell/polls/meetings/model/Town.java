package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Created by Ján Korčák on 6.3.2017.
 * email: korcak@esten.sk
 */
//@Entity
public class Town {
    @JsonIgnore
    private long id;

    private String name;

//    @OneToMany(cascade = CascadeType.ALL)
//    @MapKey(name = "name")
    private Map<String, Season> seasons;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(Map<String, Season> seasons) {
        this.seasons = seasons;
    }
}
