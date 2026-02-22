package org.blackbell.polls.source.dm.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.blackbell.polls.source.dm.dto.MeetingComponentDTO;

import java.util.List;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DMMeetingResponse {

    @JsonProperty(value = "children")
    private List<MeetingComponentDTO> children;

    public List<MeetingComponentDTO> getChildren() {
        return children;
    }

    public void setChildren(List<MeetingComponentDTO> children) {
        this.children = children;
    }
}
