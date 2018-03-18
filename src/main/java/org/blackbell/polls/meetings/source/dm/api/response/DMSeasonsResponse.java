package org.blackbell.polls.meetings.source.dm.api.response;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.blackbell.polls.meetings.source.dm.dto.SeasonDTO;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DMSeasonsResponse {

    @JsonProperty(value = "Data")
    private List<SeasonDTO> seasonDTOs;

    public List<SeasonDTO> getSeasonDTOs() {
        return seasonDTOs;
    }

    public void setSeasonDTOs(List<SeasonDTO> SeasonDTOs) {
        this.seasonDTOs = SeasonDTOs;
    }

    @Override
    public String toString() {
        return "DMSeasonsResponse{" +
                ", seasonDTOs=" + seasonDTOs +
                '}';
    }
}