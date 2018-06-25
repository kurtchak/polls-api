package org.blackbell.polls.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollsDTO extends AgendaItemComponentDTO {

    @JsonProperty(value = "children")
    private List<PollDTO> pollDTOs;

    public List<PollDTO> getPollDTOs() {
        return pollDTOs;
    }

    public void setPollDTOs(List<PollDTO> pollDTOs) {
        this.pollDTOs = pollDTOs;
    }

    @Override
    public String toString() {
        return "PollsDTO{" +
                "pollDTOs=" + pollDTOs +
                '}';
    }
}
