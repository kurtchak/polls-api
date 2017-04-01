package org.blackbell.polls.meetings.dm;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.blackbell.polls.meetings.dm.dto.SeasonDTO;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DMMeetingsResponse {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<SeasonDTO> seasonDTOs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SeasonDTO> getSeasonDTOs() {
        return seasonDTOs;
    }

    public void setSeasonDTOs(List<SeasonDTO> seasonDTOs) {
        this.seasonDTOs = seasonDTOs;
    }

    @Override
    public String toString() {
        return "DMMeetingsResponse{" +
                "name='" + name + '\'' +
                ", seasonDTOs=" + seasonDTOs +
                '}';
    }
}