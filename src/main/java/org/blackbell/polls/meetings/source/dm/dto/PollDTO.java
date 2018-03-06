package org.blackbell.polls.meetings.source.dm.dto;

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

    @JsonProperty(value = "idBodProgramu")
    private String agendaItemId;

    @JsonProperty(value = "route")
    private String pollRoute;

    @JsonProperty(value = "children")
    private List<PollChoiceDTO> pollChoiceDTOs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgendaItemId() {
        return agendaItemId;
    }

    public void setAgendaItemId(String agendaItemId) {
        this.agendaItemId = agendaItemId;
    }

    public String getPollRoute() {
        return pollRoute;
    }

    public void setPollRoute(String pollRoute) {
        this.pollRoute = pollRoute;
    }

    public List<PollChoiceDTO> getPollChoiceDTOs() {
        return pollChoiceDTOs;
    }

    public void setPollChoiceDTOs(List<PollChoiceDTO> pollChoiceDTOs) {
        this.pollChoiceDTOs = pollChoiceDTOs;
    }
}
