package org.blackbell.polls.meetings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 22.2.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<PollChoiceDTO> pollChoiceDTOs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PollChoiceDTO> getPollChoiceDTOs() {
        return pollChoiceDTOs;
    }

    public void setPollChoiceDTOs(List<PollChoiceDTO> pollChoiceDTOs) {
        this.pollChoiceDTOs = pollChoiceDTOs;
    }
}
