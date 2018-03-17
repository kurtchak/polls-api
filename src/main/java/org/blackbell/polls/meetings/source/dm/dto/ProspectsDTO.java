package org.blackbell.polls.meetings.source.dm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by Ján Korčák on 4.3.2017.
 * email: korcak@esten.sk
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProspectsDTO extends AgendaItemComponentDTO {

    @JsonProperty(value = "children")
    private List<ProspectDTO> prospectDTOs;

    public List<ProspectDTO> getProspectDTOs() {
        return prospectDTOs;
    }

    public void setProspectDTOs(List<ProspectDTO> prospectDTOs) {
        this.prospectDTOs = prospectDTOs;
    }
}
