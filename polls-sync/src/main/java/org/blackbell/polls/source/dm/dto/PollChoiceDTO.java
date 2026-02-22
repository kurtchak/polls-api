package org.blackbell.polls.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollChoiceDTO {

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "children")
    private List<CouncilMemberDTO> members;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CouncilMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<CouncilMemberDTO> members) {
        this.members = members;
    }
}
