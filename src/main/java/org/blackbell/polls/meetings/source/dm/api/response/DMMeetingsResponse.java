package org.blackbell.polls.meetings.source.dm.api.response;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.blackbell.polls.meetings.source.dm.dto.SeasonMeetingsDTO;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DMMeetingsResponse {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<SeasonMeetingsDTO> seasonMeetingsDTOs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SeasonMeetingsDTO> getSeasonMeetingsDTOs() {
        return seasonMeetingsDTOs;
    }

    public void setSeasonMeetingsDTOs(List<SeasonMeetingsDTO> seasonMeetingsDTOs) {
        this.seasonMeetingsDTOs = seasonMeetingsDTOs;
    }

    @Override
    public String toString() {
        return "DMMeetingsResponse{" +
                "name='" + name + '\'' +
                ", seasonMeetingsDTOs=" + seasonMeetingsDTOs +
                '}';
    }
}