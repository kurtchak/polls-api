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

    @JsonProperty(value = "data")
    private List<SeasonDTO> SeasonDTOs;

    public List<SeasonDTO> getSeasonDTOs() {
        return SeasonDTOs;
    }

    public void setSeasonDTOs(List<SeasonDTO> SeasonDTOs) {
        this.SeasonDTOs = SeasonDTOs;
    }

    @Override
    public String toString() {
        return "DMSeasonsResponse{" +
                ", SeasonDTOs=" + SeasonDTOs +
                '}';
    }
}