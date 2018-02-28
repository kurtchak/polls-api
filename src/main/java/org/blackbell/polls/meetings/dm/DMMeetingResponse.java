package org.blackbell.polls.meetings.dm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.blackbell.polls.meetings.dm.dto.AgendaDTO;
import org.blackbell.polls.meetings.dm.dto.AttachmentsDTO;
import org.blackbell.polls.meetings.dm.dto.MeetingComponentDTO;

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
