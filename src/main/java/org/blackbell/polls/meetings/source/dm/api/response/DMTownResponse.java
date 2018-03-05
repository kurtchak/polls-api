package org.blackbell.polls.meetings.source.dm.api.response;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.blackbell.polls.meetings.source.dm.dto.TownDTO;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DMTownResponse {

    @JsonProperty(value = "data")
    private List<TownDTO> townDTOs;

    public List<TownDTO> getTownDTOs() {
        return townDTOs;
    }

    public void setTownDTOs(List<TownDTO> townDTOs) {
        this.townDTOs = townDTOs;
    }

    @Override
    public String toString() {
        return "DMMeetingsResponse{" +
                ", townDTOs=" + townDTOs +
                '}';
    }
}