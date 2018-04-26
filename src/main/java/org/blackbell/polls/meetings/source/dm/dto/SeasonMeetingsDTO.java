package org.blackbell.polls.meetings.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonMeetingsDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<SeasonMeetingDTO> seasonMeetingDTOs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SeasonMeetingDTO> getSeasonMeetingDTOs() {
        return seasonMeetingDTOs;
    }

    public void setSeasonMeetingDTOs(List<SeasonMeetingDTO> seasonMeetingDTOs) {
        this.seasonMeetingDTOs = seasonMeetingDTOs;
    }

    @Override
    public String toString() {
        return "SeasonMeetingsDTO{" +
                "name='" + name + '\'' +
                ", seasonSeasonMeetingDTOs=" + seasonMeetingDTOs +
                '}';
    }
}
