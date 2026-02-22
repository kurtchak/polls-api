package org.blackbell.polls.source.dm.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.blackbell.polls.source.dm.dto.VoterDTO;

import java.util.List;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DMPollDetailResponse {

    @JsonProperty(value = "children")
    private List<VoterDTO> children;

    public List<VoterDTO> getChildren() {
        return children;
    }

    public void setChildren(List<VoterDTO> children) {
        this.children = children;
    }
}
